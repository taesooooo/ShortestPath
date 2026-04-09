package com.shortestpath.shortestpath.common;

public class PageInfo {
    private int page; // 현재 페이지 번호 (0부터 시작)
    private int size; // 페이지당 요소 수
    private int offset; // 쿼리에 쓰일 오프셋  

    public PageInfo() {
        
    }
    
    public PageInfo(int page, int size) {
        this.page = page;
        this.size = size;
        this.offset = page * size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return offset;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    
}
