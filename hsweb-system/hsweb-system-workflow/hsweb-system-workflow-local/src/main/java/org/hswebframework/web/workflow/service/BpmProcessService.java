package org.hswebframework.web.workflow.service;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.hswebframework.web.workflow.service.request.StartProcessRequest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 流程实例操作相关接口
 * @Author wangwei
 * @Date 2017/8/4.
 */
public interface BpmProcessService{

    /**
     * 获取所有可以启动的流程
     * @return
     */
    List<ProcessDefinition> getAllProcessDefinition();

    /**
     * 启动一个流程，并初始化自定义主表单数据
     *
     * @return 启动后的流程实例
     */
    ProcessInstance startProcessInstance(StartProcessRequest request);

    /**
     * 流程实例挂起
     * @param procInstId 流程实例ID
     */
    void closeProcessInstance(String procInstId);

    /**
     * 流程实例激活
     * @param procInstId 流程实例ID
     */
    void openProcessInstance(String procInstId);

    /**
     * 根据流程定义id获取流程定义实例<br/>
     * 此方法使用了缓存，返回的{@link org.activiti.engine.repository.ProcessDefinition}实例不为activity默认的实例，而是{@link ProcessDefinitionCache},以保证缓存时正常序列化
     *
     * @param procDefId 流程定义id
     * @return 流程定义实例
     * @throws Exception 异常信息
     */
    ProcessDefinition getProcessDefinitionById(String procDefId);

    /**
     * 根据流程定义id获取流程定义实例<br/>
     * 此方法使用了缓存，返回的{@link org.activiti.engine.repository.ProcessDefinition}实例不为activity默认的实例，而是{@link ProcessDefinitionCache},以保证缓存时正常序列化
     *
     * @param procDefKey 流程定义id
     * @return 流程定义实例
     * @throws Exception 异常信息
     */
    ProcessDefinition getProcessDefinitionByKey(String procDefKey);

    /***
     * 获取job事件
     * @param procInstId  执行ID
     */
    Job getJob(String procInstId);

    /***
     * 强制删除job任务,该方法无 api 接口
     * @param jobId
     */
    int deleteJob(String jobId);

    /***
     * 查看流程图
     * @param procDefId     流程定义id
     * @return
     */
    InputStream findProcessPic(String procDefId);
}
