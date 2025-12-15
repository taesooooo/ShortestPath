package com.shortestpath.shortestpath.entity;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeIndex {
    @Id
    private int id;
    private Coordinate coordinate;
    private int offset;
}
