package com.shortestpath;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import com.shortestpath.shortestpath.ShortestPathApplication;
import com.shortestpath.shortestpath.TestRootContext;
import com.shortestpath.shortestpath.config.RootContext;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = "com.shortestpath",
    excludeFilters = {
        @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = RootContext.class),
        @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = ShortestPathApplication.class)
    }
)
// @Import(TestRootContext.class)
public class TestApplication {
    
}
