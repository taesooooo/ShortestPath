package com.shortestpath.shortestpath.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.entity.NodeIndex;

@Repository
public interface NodeIndexRepository extends JpaRepository<NodeIndex, Integer> {

    public Optional<NodeIndex> findByCoordinate(Coordinate coordinate);
}
