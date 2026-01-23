package com.shortestpath.shortestpath;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import com.mysql.cj.xdevapi.SessionFactory;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Component
public class DBHelper {
    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tables;

    @PostConstruct
    public void init() {

    }
    
    @Transactional
    public void turncate() {
        entityManager.createNativeQuery("TRUNCATE TABLE node_index").executeUpdate();
    }
}
