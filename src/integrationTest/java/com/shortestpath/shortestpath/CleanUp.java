package com.shortestpath.shortestpath;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

import jakarta.annotation.PreDestroy;

@Component
public class CleanUp {
    private static final Logger log = LoggerFactory.getLogger(CleanUp.class);
    
    private JdbcTemplate jdbcTemplate;
    private Engine engine;


    public CleanUp(Engine engine, JdbcTemplate jdbcTemplate) {
        this.engine = engine;
        this.jdbcTemplate = jdbcTemplate;
    }

   // 모든 테스트 클래스가 공유 클래스를 사용하고 마지막에 컨텍스트가 종료 될때 DB도 초기화
	@PreDestroy
	public void cleanup() throws IOException {
		jdbcTemplate.update("TRUNCATE TABLE node_index;");
        IntegrationTestHelper.deleteBinaryFiles(((HybridDataStore) engine.getStore()));
		// engine.getStore().close();

		log.info("테스트 공유 컨텍스트 종료 후 DB 삭제 및 생성 데이터 삭제 완료");
	}     
}
