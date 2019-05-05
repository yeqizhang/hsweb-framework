package org.hswebframework.web.datasource.manager.simple;

import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.executor.SqlExecutor;
import org.hswebframework.web.database.manager.SqlExecuteRequest;
import org.hswebframework.web.database.manager.SqlExecuteResult;
import org.hswebframework.web.database.manager.SqlInfo;
import org.hswebframework.web.database.manager.exception.SqlExecuteException;
import org.hswebframework.web.datasource.DataSourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 默认的事务执行器
 *
 * @author zhouhao
 */
public class DefaultLocalTransactionExecutor implements TransactionExecutor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private BlockingQueue<Execution> executionQueue = new LinkedBlockingQueue<>();

    private SqlExecutor sqlExecutor;

    private SqlRequestExecutor sqlRequestExecutor;

    private String transactionId;

    private String datasourceId;

    private volatile boolean shutdown = false;

    private TransactionStatus transactionStatus;

    private TransactionTemplate transactionTemplate;

    private volatile boolean commit = false;

    private volatile boolean running = false;

    private CountDownLatch waitClose = new CountDownLatch(1);

    public DefaultLocalTransactionExecutor(SqlExecutor sqlExecutor, String transactionId, String datasourceId, TransactionTemplate transactionTemplate) {
        this.sqlExecutor = sqlExecutor;
        this.transactionId = transactionId;
        this.datasourceId = datasourceId;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String getDatasourceId() {
        return datasourceId;
    }

    @Override
    public void commit() {
        commit = true;
        shutdown = true;
        waitToClose();
    }

    protected void waitToClose() {
        try {
            executionQueue.add(new NullExecution());
            logger.debug("wait transaction {} close", transactionId);
            //等待执行结束
            waitClose.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void rollback() {
        tryRollback();
        waitToClose();
    }

    private void tryRollback() {
        running = false;
        shutdown = true;
        commit = false;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    public void setSqlRequestExecutor(SqlRequestExecutor sqlRequestExecutor) {
        this.sqlRequestExecutor = sqlRequestExecutor;
    }

    protected void buildDefaultSqlRequestExecutor() {
        sqlRequestExecutor = (executor, sqlInfo) -> new NonTransactionSqlExecutor(executor).doExecute(sqlInfo);
    }

    @Override
    public void run() {
        try {
            if (datasourceId != null) {
                DataSourceHolder.switcher().use(datasourceId);
            }
            //开启事务
            transactionStatus = transactionTemplate.getTransactionManager().getTransaction(transactionTemplate);
            if (sqlRequestExecutor == null) {
                buildDefaultSqlRequestExecutor();
            }
            while (!shutdown) {
                logger.debug("wait sql execute request {}", transactionId);
                Execution execution;
                if (transactionTemplate.getTimeout() > 0) {
                    execution = executionQueue.poll(transactionTemplate.getTimeout(), TimeUnit.MILLISECONDS);//等待有新的sql进来
                    if (execution == null) {
                        throw new TimeoutException("事务[" + transactionId + "]超时");
                    }
                } else {
                    execution = executionQueue.take();
                }
                if (execution instanceof NullExecution) {
                    continue;
                }
                //执行sql
                doExecute(execution);
            }
        } catch (Exception e) {
            tryRollback();//回滚
            logger.error("execute sql error {}", transactionId, e);
        } finally {
            try {
                if (commit) {
                    logger.debug("Commit transaction {}", transactionId);
                    transactionTemplate.getTransactionManager().commit(transactionStatus);
                } else {
                    logger.debug("Roll Back transaction {}", transactionId);
                    transactionTemplate.getTransactionManager().rollback(transactionStatus);
                }
                //结束事务
                waitClose.countDown();
            } finally {
                DataSourceHolder.switcher().reset();
            }
        }
    }

    @SneakyThrows
    protected void doExecute(Execution execution) {
        running = true;
        logger.debug("start execute sql {}", transactionId);
        try {
            List<SqlExecuteResult> requests = execution.request.getSql()
                    .stream()
                    .map(sqlInfo -> {
                        try {
                            if (execution.datasourceId != null) {
                                DataSourceHolder.switcher().use(execution.datasourceId);
                            } else {
                                DataSourceHolder.switcher().useDefault();
                            }
                            //执行sql
                            return sqlRequestExecutor.apply(sqlExecutor, sqlInfo);
                        } catch (Exception e) {
                            return SqlExecuteResult.builder()
                                    .result(e.getMessage())
                                    .sqlInfo(sqlInfo)
                                    .success(false)
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());
            //通过回调返回执行结果
            execution.callback.accept(requests);
        } catch (Exception e) {
            execution.onError.accept(e);
        } finally {
            running = false;
        }
    }

    @Override
    public List<SqlExecuteResult> execute(SqlExecuteRequest request) throws Exception {
        if (shutdown) {
            throw new UnsupportedOperationException("transaction is close");
        }
        //执行倒计时,执行sql是异步的,通过此方式等待sql执行完毕
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<SqlExecuteResult> results = new ArrayList<>();

        //异常信息
        Execution execution = new Execution();
        execution.datasourceId = DataSourceHolder.switcher().currentDataSourceId();

        execution.request = request;
        execution.callback = sqlExecuteResults -> {
            results.addAll(sqlExecuteResults);
            sqlExecuteResults.clear();
            countDownLatch.countDown();
        };
        execution.onError = (e) -> countDownLatch.countDown();
        logger.debug("submit sql execute job {}", transactionId);
        executionQueue.add(execution);
        //等待sql执行完毕
        countDownLatch.await();
        return results;
    }

    private class NullExecution extends Execution {

    }

    protected class Execution {

        protected String datasourceId;

        protected SqlExecuteRequest request;

        protected Consumer<List<SqlExecuteResult>> callback;

        protected Consumer<Exception> onError;
    }

    private interface SqlRequestExecutor {
        SqlExecuteResult apply(SqlExecutor executor, SqlInfo sqlInfo) throws SQLException;
    }

}
