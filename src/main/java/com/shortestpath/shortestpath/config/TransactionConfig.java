package com.shortestpath.shortestpath.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 트랜잭션 관리 설정
 * 다중 데이터소스에서의 트랜잭션 처리를 위한 보조 설정
 */
@Configuration
public class TransactionConfig {

    /**
     * MySQL 트랜잭션 템플릿
     * 진행 중인 트랜잭션에 대한 프로그래밍 방식의 제어
     */
    @Bean("mysqlTransactionTemplate")
    public TransactionTemplate mysqlTransactionTemplate(
            @Qualifier("mysqlTransactionManager") JpaTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
