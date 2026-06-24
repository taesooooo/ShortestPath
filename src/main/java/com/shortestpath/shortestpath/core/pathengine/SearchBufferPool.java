package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class SearchBufferPool {
    private final BlockingQueue<SearchBufferPair> pool;
    private final int bufferCapacity;

    public SearchBufferPool(int poolSize, int bufferCapacity) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("SearchBuffer pool size는 1 이상이어야 합니다.");
        }
        if (bufferCapacity < 1) {
            throw new IllegalArgumentException("SearchBuffer capacity는 1 이상이어야 합니다.");
        }

        this.pool = new ArrayBlockingQueue<>(poolSize);
        this.bufferCapacity = bufferCapacity;

        for (int i = 0; i < poolSize; i++) {
            pool.add(new SearchBufferPair(new SearchBuffers(bufferCapacity), new SearchBuffers(bufferCapacity)));
        }
    }

    public SearchBufferPair take() throws IOException {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SearchBuffer를 빌리는 중 인터럽트가 발생했습니다.", e);
        }
    }

    public void release(SearchBufferPair bufferPair) {
        if (bufferPair != null) {
            pool.offer(bufferPair);
        }
    }

    public int bufferCapacity() {
        return bufferCapacity;
    }

    public static final class SearchBufferPair {
        private final SearchBuffers forwardBuffer;
        private final SearchBuffers reverseBuffer;

        private SearchBufferPair(SearchBuffers forwardBuffer, SearchBuffers reverseBuffer) {
            this.forwardBuffer = forwardBuffer;
            this.reverseBuffer = reverseBuffer;
        }

        public SearchBuffers forwardBuffer() {
            return forwardBuffer;
        }

        public SearchBuffers reverseBuffer() {
            return reverseBuffer;
        }
    }
}
