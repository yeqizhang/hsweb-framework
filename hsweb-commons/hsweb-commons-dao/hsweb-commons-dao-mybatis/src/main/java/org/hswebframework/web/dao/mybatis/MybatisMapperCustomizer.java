package org.hswebframework.web.dao.mybatis;

/**
 * 排除不需要加载的mapper.xml
 *
 * @author zhouhao
 * @since 3.0
 */
public interface MybatisMapperCustomizer {
    String[] getExcludes();

    String[] getIncludes();
}
