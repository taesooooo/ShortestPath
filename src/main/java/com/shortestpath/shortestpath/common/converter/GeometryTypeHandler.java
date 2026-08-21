package com.shortestpath.shortestpath.common.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL의 geometry 컬럼과 JTS Point를 양방향 변환하는 MyBatis TypeHandler
 * 
 * - 조회(SELECT): Geometry → Point
 * - 저장(INSERT/UPDATE): Point → Geometry
 */
@MappedTypes(Point.class)
@MappedJdbcTypes(JdbcType.BINARY)
public class GeometryTypeHandler extends BaseTypeHandler<Point> {

    private static final WKBReader wkbReader = new WKBReader();

    /**
     * PreparedStatement에 Point 값을 설정 (INSERT/UPDATE 시)
     * Point를 WKB 형식의 Geometry로 변환
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Point parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setObject(i, null);
            return;
        }
        
        // Point를 WKB(바이너리) 형식으로 변환;
        ps.setBytes(i, new WKBWriter().write(parameter));
    }

    /**
     * ResultSet에서 Point 값을 읽기 (SELECT 시)
     */
    @Override
    public Point getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return convertWkbToPoint(bytes);
    }

    /**
     * ResultSet에서 Point 값을 읽기 (SELECT 시 - 인덱스로 접근)
     */
    @Override
    public Point getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return convertWkbToPoint(bytes);
    }

    /**
     * CallableStatement에서 Point 값을 읽기 (프로시저 호출 시)
     */
    @Override
    public Point getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return convertWkbToPoint(bytes);
    }

    /**
     * WKB(바이너리) 형식의 Geometry를 Point로 변환
     */
    private Point convertWkbToPoint(byte[] wkbGeometry) {
        if (wkbGeometry == null || wkbGeometry.length == 0) {
            return null;
        }

        try {
            Geometry geometry = wkbReader.read(wkbGeometry);
            if (geometry instanceof Point) {
                return (Point) geometry;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
