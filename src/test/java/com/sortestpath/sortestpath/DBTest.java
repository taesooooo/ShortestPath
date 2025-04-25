package com.sortestpath.sortestpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = DataSourceAutoConfiguration.class) // 필요한 설정만 로드
class DBTest {
	private static final Logger log = LoggerFactory.getLogger(DBTest.class);


	@Autowired
	private DataSource dataSource;
	
	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void connetTest() throws SQLException {
		Connection conn = dataSource.getConnection();
		log.info(conn.getMetaData().getURL());	
		log.info(conn.getMetaData().getUserName());
		
		assertThat(conn).isNotNull();
	}

}
