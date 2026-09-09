package com.shortestpath.shortestpath.common.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;

import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler: 비트패킹된 long 좌표를 PostGIS geometry로 변환
 * 
 * IndexInfo.coordinate (long 비트패킹)을 받아서
 * → Coordinate 객체로 변환
 * → Point로 변환
 * → WKB 바이너리로 변환
 * → 데이터베이스에 저장
 * 
 * XML 매퍼에서:
 *   #{item.coordinate, javaType=java.lang.Long, typeHandler=LongToGeometryTypeHandler}
 */
@MappedTypes(Long.class)
@MappedJdbcTypes(JdbcType.BINARY)
public class LongToGeometryTypeHandler extends BaseTypeHandler<Long> {

    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final WKBWriter wkbWriter = new WKBWriter();

    /**
     * PreparedStatement에 long 좌표 값을 설정 (INSERT/UPDATE 시)
     * 
     * 1. long (비트패킹) → Coordinate
     * 2. Coordinate → Point
     * 3. Point → WKB (바이너리)
     * 4. PS에 바이너리 저장
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Long coordinateLong, JdbcType jdbcType) throws SQLException {
        if (coordinateLong == null) {
            ps.setObject(i, null);
            return;
        }

        try {
            // 1. long → Coordinate 변환 (비트언패킹)
            org.locationtech.jts.geom.Coordinate coordinate = GeometryUtil.longToCoordinate(coordinateLong);
            
            // 2. Coordinate → Point 변환
            Point point = geometryFactory.createPoint(coordinate);
            point.setSRID(4326);
            
            // 3. Point → WKB 바이너리 변환
            byte[] wkbBytes = wkbWriter.write(point);
            
            // 4. 데이터베이스에 저장
            ps.setBytes(i, wkbBytes);
        } catch (Exception e) {
            throw new SQLException("좌표 변환 실패: " + coordinateLong, e);
        }
    }

    /**
     * ResultSet에서 값을 읽기 (SELECT 시)
     * 이 핸들러는 쓰기 전용이므로 구현 불필요
     */
    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return null;
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }
}
