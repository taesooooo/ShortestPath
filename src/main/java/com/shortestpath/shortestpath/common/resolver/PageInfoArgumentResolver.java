package com.shortestpath.shortestpath.common.resolver;

import com.shortestpath.shortestpath.common.PageInfo;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PageInfoArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(PageInfo.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        
        String page = webRequest.getParameter("page");
        String size = webRequest.getParameter("size");
        
        int pageNum = page != null ? Integer.parseInt(page) : 0;
        int pageSize = size != null ? Integer.parseInt(size) : 10;
        
        return new PageInfo(pageNum, pageSize);
    }
}