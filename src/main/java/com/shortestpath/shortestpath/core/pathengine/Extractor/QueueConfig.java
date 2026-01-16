package com.shortestpath.shortestpath.core.pathengine.Extractor;

public class QueueConfig {
    public static int calculateSafeCapacity(int estimatedObjectSize) {
        // 1. 현재 JVM이 사용할 수 있는 최대 메모리 (Byte)
        long maxMemory = Runtime.getRuntime().maxMemory();
        
        // 3. 전체 메모리의 80% 정도만 큐에 할당하도록 계산
        double safetyFactor = 0.8;
        long targetMemory = (long) (maxMemory * safetyFactor);
        
        int capacity = (int) (targetMemory / estimatedObjectSize);
    
        return Math.max(1000, capacity);
    }
}
