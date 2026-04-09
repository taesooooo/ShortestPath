package com.shortestpath.shortestpath.common;

import java.util.List;

public class Page<T> {
    long totalElements; // 전체 요소 수
    int totalPages; // 전체 페이지 수
    int currentPage; // 현재 페이지 번호 (0부터 시작)
    int pageSize; // 페이지당 요소 수
    List<T> content; // 현재 페이지의 요소 리스트

    public Page() {
        
    }

    public Page(long totalElements, PageInfo pageInfo, List<T> content) {
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageInfo.getSize());
        this.currentPage = pageInfo.getPage();
        this.pageSize = pageInfo.getSize();
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<T> getContent() {
        return content;
    }
}
