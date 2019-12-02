package com.zhur.demo.controller;

import com.zhur.demo.service.IGPService;
import com.zhur.mvcframework.annotation.GPAutowride;
import com.zhur.mvcframework.annotation.GPController;
import com.zhur.mvcframework.annotation.GPRequestMapping;
import com.zhur.mvcframework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @program: spring-1.0
 * @description: 测试Controller
 * @author: zhur
 * @date: 2019-11-29 21:01
 **/
@GPController
@GPRequestMapping("/demo")
public class Controller {

        @GPAutowride
        private IGPService igpService;

        @GPRequestMapping("/query")
        public void query(HttpServletRequest req, HttpServletResponse resp,
                          @GPRequestParam("name") String name) {
            String result = igpService.get(name);
            try {
                resp.getWriter().write(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
