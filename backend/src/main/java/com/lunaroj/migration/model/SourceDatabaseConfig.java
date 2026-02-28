package com.lunaroj.migration.model;

import lombok.Data;

@Data
public class SourceDatabaseConfig {

    private String host = "127.0.0.1";

    private Integer port = 3306;

    private String db = "scnuoj";

    private String user;

    private String password;
}

