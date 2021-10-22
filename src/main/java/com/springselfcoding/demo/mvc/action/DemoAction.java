package com.springselfcoding.demo.mvc.action;

import com.springselfcoding.demo.service.IDemoService;
import com.springselfcoding.mvcframework.SelfAutowired;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfRequestMapping;
import com.springselfcoding.mvcframework.SelfRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SelfController
@SelfRequestMapping("/demo")
public class DemoAction {

    @SelfAutowired
    private IDemoService demoService;

    @SelfRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp, @SelfRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
