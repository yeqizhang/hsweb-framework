package org.hswebframework.web.datasource.switcher;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TODO 完成注释
 *
 * @author zhouhao
 */
public class DefaultDataSourceSwitcherTest {

    DataSourceSwitcher switcher = new DefaultDataSourceSwitcher();

    @Test
    public void testChangeSwitcher() {

        switcher.use("test");//切换为test
        assertEquals(switcher.currentDataSourceId(), "test");
        switcher.use("test2");//切换为test2
        assertEquals(switcher.currentDataSourceId(), "test2");

        switcher.useDefault();//切换默认数据源
        assertTrue(switcher.currentDataSourceId() == null);

        switcher.useLast(); //切换为上一次使用的数据源(test2)
        assertEquals(switcher.currentDataSourceId(), "test2");

        switcher.useLast(); //切换为上一次使用的数据源(test)
        assertEquals(switcher.currentDataSourceId(), "test");

        switcher.useLast(); //切换为上一次书用的数据源(无,默认为default)
        assertTrue(switcher.currentDataSourceId() == null);


        switcher.useLast();
        assertTrue(switcher.currentDataSourceId() == null);

    }

    @Test
    public void testChangeSwitcher2() {

        switcher.use("test");//切换为test
        assertEquals(switcher.currentDataSourceId(), "test");
        switcher.useDefault();
        switcher.useDefault();
        switcher.useDefault();
        assertTrue(switcher.currentDataSourceId() == null);
        switcher.useLast();
        switcher.useLast();
        switcher.useLast();
        assertEquals(switcher.currentDataSourceId(), "test");
    }

    public class Test2 extends TestClass {

    }

    public class TestClass {
        public void test() {

        }
    }
}