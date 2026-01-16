package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

import org.geotools.api.feature.simple.SimpleFeature;

public class FeatureItem implements FeatureTaskItem{
    private SimpleFeature feature;

    public FeatureItem(SimpleFeature feature) {
        this.feature = feature;
    }

    public SimpleFeature getFeature() {
        return feature;
    }
}
