package com.parkease.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;


public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String,String> customHeaders = new HashMap<>();

    public HeaderMapRequestWrapper(HttpServletRequest request){
        super(request);
    }

    public void addHeader(String name,String value){
        customHeaders.put(name,value);
    }

    @Override
    public String getHeader(String name) {

        String headerValue = customHeaders.get(name);
        return (headerValue != null) ? headerValue : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> set = new HashSet<>(customHeaders.keySet());
        Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();

        while (e.hasMoreElements()){
            set.add(e.nextElement());
        }
        return Collections.enumeration(set);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {

        String headerValue = customHeaders.get(name);

        if (headerValue != null){
            return Collections.enumeration(List.of(headerValue));
        }
        return super.getHeaders(name);
    }
}
