package com.shortestpath.shortestpath.common.converter;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CoordinateConverter implements AttributeConverter<Coordinate, Point> {
    @Override
    public Point convertToDatabaseColumn(Coordinate coordinate) {
        if (coordinate == null) {
            return null;
        }
        org.locationtech.jts.geom.Coordinate jtsCoordinate = new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude());
        return new GeometryFactory().createPoint(jtsCoordinate);
    }

    @Override
    public Coordinate convertToEntityAttribute(Point dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        return new Coordinate(dbData.getY(), dbData.getX());
    }
    
}
