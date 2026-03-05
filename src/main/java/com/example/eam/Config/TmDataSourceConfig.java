package com.example.eam.Config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class TmDataSourceConfig {

    @Bean(name = "tmDataSource")
    @ConditionalOnProperty(prefix = "spring.datasource.tm", name = "jdbc-url")
    @ConfigurationProperties(prefix = "spring.datasource.tm")
    public DataSource tmDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "tmJdbcTemplate")
    @ConditionalOnProperty(prefix = "spring.datasource.tm", name = "jdbc-url")
    public JdbcTemplate tmJdbcTemplate(@Qualifier("tmDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
