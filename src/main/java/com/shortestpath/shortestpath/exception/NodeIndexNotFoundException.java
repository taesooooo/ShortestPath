package com.shortestpath.shortestpath.exception;

public class NodeIndexNotFoundException extends RuntimeException {
    public NodeIndexNotFoundException() {
        super();
    }
    
    public NodeIndexNotFoundException(String message) {
        super(message);
    }
}
