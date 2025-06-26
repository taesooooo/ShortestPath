package com.shortestpath.shortestpath.entity;

import org.locationtech.jts.geom.Geometry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity(name = "map")
public class GeoLink {
	/*
	 * 	OGR_FID int(11) AI PK 
		SHAPE geometry 
		osm_id varchar(12) 
		code decimal(4,0) 
		fclass varchar(28) 
		name varchar(100) 
		ref varchar(20) 
		oneway varchar(1) 
		maxspeed decimal(3,0) 
		layer decimal(12,0) 
		bridge varchar(1) 
		tunnel varchar(1)
	 */
	
	@Id
	@Column(name = "OGR_FID")
	private int ogrFID;
	@Column(name = "SHAPE")
	private Geometry shape;
	@Column(name = "osm_id")
	private String osmId;
	private int code;
	private String fclass;
	private String name;
	private String ref;
	private String oneway;
	private int maxspeed;
	private int layer;
	private String bridge;
	private String tunnel;
}
