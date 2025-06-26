package com.shortestpath.shortestpath.repository;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shortestpath.shortestpath.core.pathengine.DataProvider;
import com.shortestpath.shortestpath.entity.GeoLink;

@Repository
public interface MapRepository extends JpaRepository<GeoLink, Integer> {
	@Query(value = "SELECT SHAPE "
			+ "FROM map "
			+ "WHERE mbrintersects(SHAPE, ST_GeomFromText(ST_AsText(ST_Buffer(ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'), 0), :range)), 1))", nativeQuery = true)
	public List<Geometry> findNearestLine(@Param("longitude") double longitude, @Param("latitude") double latitude, @Param("range") double range);

}
