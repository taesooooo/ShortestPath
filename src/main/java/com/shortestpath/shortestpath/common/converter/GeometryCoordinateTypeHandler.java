package com.shortestpath.shortestpath.common.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.PGProperty;
import org.postgresql.util.PGobject;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

@MappedTypes(Coordinate.class)
@MappedJdbcTypes({JdbcType.OTHER, JdbcType.BINARY})
public class GeometryCoordinateTypeHandler extends BaseTypeHandler<Coordinate> {

    private static final WKBReader WKB_READER = new WKBReader();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Coordinate parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.OTHER);
            return;
        }

        org.locationtech.jts.geom.Coordinate jtsCoordinate =
                new org.locationtech.jts.geom.Coordinate(parameter.getLongitude(), parameter.getLatitude());
        Point point = new org.locationtech.jts.geom.GeometryFactory().createPoint(jtsCoordinate);
        ps.setBytes(i, new WKBWriter().write(point));
    }

    @Override
    public Coordinate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toCoordinate(rs.getObject(columnName));
    }

    @Override
    public Coordinate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toCoordinate(rs.getObject(columnIndex));
    }

    @Override
    public Coordinate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toCoordinate(cs.getObject(columnIndex));
    }

    private Coordinate toCoordinate(Object value) throws SQLException {
        if (value == null) {
            return null;
        }

        try {
            if(value instanceof PGobject) {
                PGobject pgObject = (PGobject) value;
                byte[] wkbBytes = HexFormat.of().parseHex(pgObject.getValue());
                Geometry geometry = new WKBReader().read(wkbBytes);

                if (geometry instanceof Point point) {
                    return new Coordinate(point.getY(), point.getX());
                }
            }
        } catch (Exception e) {
            throw new SQLException("변환하는중 에러가 발생했습니다. " + e.getMessage(), e);
        }

        throw new SQLException("지원되지 않는 geometry 값 유형입니다: " + value.getClass().getName());
    }
}
