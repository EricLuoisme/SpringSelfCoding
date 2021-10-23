package com.springselfcoding.demo;

import com.springselfcoding.ioc.context.SelfApplicationContext;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * V2版本将IoC注入, WebMVC, AOP功能拆分出去
 */
public class SelfDispatchServletV2 extends HttpServlet {

    // 存放Url和Controller的处理方法, 进行绑定
    private Map<String, Method> handlerMapper = new HashMap<>();

    // 声明ApplicationContext, IoC容器的访问上下文
    private SelfApplicationContext applicationContext = null;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 6. 根据URL委派给具体调用方法
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Error");
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 直接将加载配置文件, 扫描相关类, 初始化IoC容器, Bean实例化, 依赖注入, 全部交给ApplicationContext处理
        applicationContext = new SelfApplicationContext(config.getInitParameter("contextConfigLocation"));

        // ------------- MVC ------------------
        // 5. 初始化HandlerMapping (url和method建立关系)
        doInitHandlerMapping();

        System.out.println("Self Dispatch Servlet finished initialization");

    }

    /**
     * 建立Url和Method的关联
     */
    private void doInitHandlerMapping() {
        if (0 == this.applicationContext.getBeanDefinitionCount()) {
            return;
        }

        for (String beanName : this.applicationContext.getBeanDefinitionNames()) {

            Object ins = this.applicationContext.getBean(beanName);
            Class<?> clazz = ins.getClass();

            // 只对Controller进行处理
            if (clazz.isAnnotationPresent(SelfController.class)) {

                String baseUrl = "";
                // 判断Clazz有没有加RequestMapping
                if (clazz.isAnnotationPresent(SelfRequestMapping.class)) {
                    SelfRequestMapping baseRequestMapping = clazz.getAnnotation(SelfRequestMapping.class);
                    baseUrl = baseRequestMapping.value();
                }


                // 仅迭代Public的方法
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(SelfRequestMapping.class)) {
                        SelfRequestMapping requestMapping = method.getAnnotation(SelfRequestMapping.class);
                        String url = (baseUrl + requestMapping.value());

                        handlerMapper.put(url, method);
                        System.out.println("Mapped: " + url + " ---> " + method);
                    }
                }
            }

        }
    }

    /**
     * 找到对应HandlerMapping并调用处理
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (!this.handlerMapper.containsKey(url)) {
            // 不存在页面
            resp.getWriter().write("404 Not Found");
            return;
        }

        Method method = this.handlerMapper.get(url);
        Map<String, String[]> parameterMap = req.getParameterMap();

        // 这里就是任务只使用默认名称, 通过这个方式获取beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(this.applicationContext.registry.factoryBeanInstanceCache.get(beanName),
                new Object[]{req, resp, parameterMap.get("name")[0]});
    }

    /**
     * 只转首字母为小写
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
