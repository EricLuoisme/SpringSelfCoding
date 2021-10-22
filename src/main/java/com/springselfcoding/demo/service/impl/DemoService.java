package com.springselfcoding.demo.service.impl;


import com.springselfcoding.mvcframework.SelfService;
import com.springselfcoding.demo.service.IDemoService;

@SelfService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
