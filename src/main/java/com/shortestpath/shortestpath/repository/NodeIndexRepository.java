package com.shortestpath.shortestpath.repository;

import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.entity.NodeIndex;

@Repository
public interface NodeIndexRepository extends JpaRepository<NodeIndex, Integer> {

    public Optional<NodeIndex> findByCoordinate(Coordinate coordinate);

    @Query(value = "SELECT offset FROM node_index WHERE id = :id", nativeQuery = true)
    public int findOffsetById(int id);

    @Query(value = 
        """
        SELECT id, coordinate, ST_Distance_Sphere(coordinate, ST_SRID(POINT(:#{#coordinate.longitude},:#{#coordinate.latitude}), 4326)) AS distance, offset 
        FROM node_index 
        WHERE ST_Within(coordinate, ST_GeomFromText(:bbox, 4326)) 
        ORDER BY distance 
        LIMIT 5;
        """, nativeQuery = true)
    public List<NodeIndex> findNearestNode(@Param("bbox")String bboxWkt, @Param("coordinate")Coordinate coordinate);
}
