package com.zhur.demo.service.impl;

import com.zhur.demo.service.IGPService;
import com.zhur.mvcframework.annotation.GPService;


/**
 * @program: spring-1.0
 * @description: 实现类
 * @author: zhur
 * @date: 2019-11-29 20:55
 **/

@GPService
public class Service implements IGPService {

    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
