package com.shortestpath.shortestpath.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestBBox {
    double minlon;
    double minlat;
    double maxlon;
    double maxlat;
}
