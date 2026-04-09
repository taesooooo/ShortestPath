package com.shortestpath.shortestpath.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

/**
 * 다중 데이터소스 설정
 * - MySQL: JPA, JdbcTemplate 사용 (Entity 관리)
 * - PostgreSQL: MyBatis 사용 (매퍼 기반)
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.shortestpath.shortestpath.repository", // JPA Repository
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "mysqlTransactionManager"
)
@MapperScan(
    basePackages = "com.shortestpath.shortestpath.mapper",
    sqlSessionFactoryRef = "postgresSqlSessionFactory"
)
public class DataSourceConfig {

    /**
     * MySQL DataSource (Primary - JPA, JdbcTemplate 사용)
     */
    @Primary
    @Bean("mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * MySQL Entity Manager Factory
     */
    @Primary
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.shortestpath.shortestpath.entity", "com.shortestpath.shortestpath.common.converter")
                .persistenceUnit("mysql")
                .build();
    }

    /**
     * MySQL JPA Transaction Manager
     */
    @Primary
    @Bean("mysqlTransactionManager")
    public PlatformTransactionManager mysqlTransactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * MySQL JdbcTemplate
     */
    @Primary
    @Bean("mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ======================== PostgreSQL DataSource (MyBatis) ========================

    /**
     * PostgreSQL DataSource (MyBatis 사용)
     */
    @Bean("postgresqlDataSource")
    @ConfigurationProperties(prefix = "mybatis.postgresql")
    public DataSource postgresqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * PostgreSQL MyBatis SqlSessionFactory
     */
    @Bean("postgresSqlSessionFactory")
    public SqlSessionFactory postgresSqlSessionFactory(
            @Qualifier("postgresqlDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/**/*.xml"));
        sqlSessionFactoryBean.setTypeAliasesPackage("com.shortestpath.shortestpath.entity");
        return sqlSessionFactoryBean.getObject();
    }

    /**
     * PostgreSQL MyBatis SqlSessionTemplate
     */
    @Bean("postgresSqlSessionTemplate")
    public SqlSessionTemplate postgresSqlSessionTemplate(
            @Qualifier("postgresSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * PostgreSQL JdbcTemplate (필요시 사용)
     */
    // @Bean("postgresJdbcTemplate")
    // public JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresqlDataSource") DataSource dataSource) {
    //     return new JdbcTemplate(dataSource);
    // }
}
