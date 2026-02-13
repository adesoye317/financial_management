package com.financal.mgt.Financal.Management.config;


import com.financal.mgt.Financal.Management.util.AESUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@Slf4j
public class DataSourceConfig {


    private final Environment environment;


    @Autowired
    public DataSourceConfig(Environment env) {
        this.environment = env;
    }

    @Bean
    @Qualifier("engDataSource")
    public DataSource engDataSource() throws Exception {

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name"));
        hikariConfig.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        hikariConfig.setUsername(AESUtil.decryptTextUsingAES(Objects.requireNonNull(environment.getProperty("spring.datasource.username")), environment.getProperty("secret.key")));
        hikariConfig.setPassword(AESUtil.decryptTextUsingAES(Objects.requireNonNull(environment.getProperty("spring.datasource.password")), environment.getProperty("secret.key")));

        hikariConfig.setMaximumPoolSize(Integer.parseInt(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.maximumPoolSize"))));
//        hikariConfig.setConnectionTestQuery(environment.getProperty("spring.datasource.hikari.connection-test-query"));
        hikariConfig.setPoolName(environment.getProperty("spring.datasource.hikari.poolName"));
        hikariConfig.setMinimumIdle(Integer.parseInt(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.minimumIdle"))));
        hikariConfig.setIdleTimeout(Long.parseLong(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.idleTimeout"))));
        hikariConfig.setConnectionTimeout(Long.parseLong(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.connectionTimeout"))));
        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", "true");
        try {
            hikariConfig.setMaxLifetime(Long.parseLong(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.maxLifeTime"))));
        }catch (Exception e){
            log.info("maxLifeTime may not be set , try maxLifetime");
            hikariConfig.setMaxLifetime(Long.parseLong(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.maxLifetime"))));
        }
        // hikariConfig.setConnectionInitSql(environment.getProperty("spring.datasource.hikari.connectionInitSql"));
        hikariConfig.setValidationTimeout(3000);
//        hikariConfig.setLeakDetectionThreshold(Long.parseLong(Objects.requireNonNull(environment.getProperty("spring.datasource.hikari.leakDetectionThreshold"))));
        return new HikariDataSource(hikariConfig);
    }

    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}

