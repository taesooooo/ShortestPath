package com.shortestpath.shortestpath.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shortestpath.shortestpath.dto.request.RequestBBox;
import com.shortestpath.shortestpath.entity.Restaurants;

public interface RestaurantsRepository extends JpaRepository<Restaurants, Long> {
    // @Query("""
    //         SELECT * FROM public.food_store WHERE sals_stts_nm like '%영업%' AND ST_Intersects(geometry,ST_MakeEnvelope(
    //         #{#bbox.minlon}, #{#bbox.minlat},
    //         #{#bbox.maxlon}, #{#bbox.maxlat},
    //         5174));""")
    // public List<Restaurants> findRestaurantsByBBox(@Param("bbox") RequestBBox bbox);
}
