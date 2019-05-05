package org.hswebframework.web.datasource.config;

import lombok.Data;
import org.hswebframework.web.datasource.DatabaseType;

import java.io.Serializable;

@Data
public class DynamicDataSourceConfig implements Serializable {
    private static final long serialVersionUID = 2776152081818934459L;

    private String id;

    private String name;

    private String describe;

    private DatabaseType databaseType;
}
