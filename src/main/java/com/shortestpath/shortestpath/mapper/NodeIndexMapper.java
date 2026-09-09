package com.shortestpath.shortestpath.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.model.NodeIndex;

@Mapper
public interface NodeIndexMapper {

	Optional<NodeIndex> findByCoordinate(@Param("coordinate") Coordinate coordinate);

	int findOffsetById(@Param("id") int id);

	List<NodeIndex> findNearestNode(@Param("bbox") String bboxWkt,
			@Param("coordinate") Coordinate coordinate);

	/**
	 * 배치로 노드 인덱스 삽입
	 * @param indexList 삽입할 노드 인덱스 리스트
	 */
	void insertNodeIndexBatch(@Param("indexList") List<IndexInfo> indexList);
}
