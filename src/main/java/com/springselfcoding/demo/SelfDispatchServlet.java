package com.springselfcoding.demo;

import com.springselfcoding.mvcframework.SelfAutowired;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class SelfDispatchServlet extends HttpServlet {

    // 加载配置
    private Properties contextConfig = new Properties();

    // 存放被扫描的类名
    private List<String> classNames = new ArrayList<>();

    // 存放实例
    private Map<String, Object> ioc = new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 6. 根据URL委派给具体调用方法
        doDispatch();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1. 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2. 扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        // ------------- IOC ------------------
        // 3. 初始化IOC容器, 并将扫描的类实例化, 缓存到IOC容器中
        doInstance();

        // ------------- DI -------------------
        // 4. 完成依赖注入
        doAutowired();

        // ------------- MVC ------------------
        // 5. 初始化HandlerMapping (url和method建立关系)
        doInitHandlerMapping();

        System.out.println("Self Dispatch Servlet finished initialization");

    }

    /**
     * 根据ContextConfigLocation这个名称, 去ClassPath下找到对应的配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描包路径下找所有文件
     */
    private void doScanner(String scanPackage) {
        // 将包路径替换为文件夹路径
        URL resource = this.getClass().getClassLoader()
                .getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(resource.getPath());

        for (File file : classPath.listFiles()) {
            // 如果是文件夹则递归
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                // 包名.类名, e.g. com.springselfcoding.demo.abc
                String className = scanPackage + "." + file.getName().replace(".class", "");
                // 将类名存入
                classNames.add(className);
            }
            // 不是class结尾的不进行处理
        }
    }

    private void doInstance() {
        if (!classNames.isEmpty()) {
            try {
                for (String className : classNames) {
                    // 根据类名找到Class, 这里的是包含路径的全类名
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(SelfController.class)) {
                        // 获取名称并实例化
                        String beanName = toLowerFirstCase(clazz.getSimpleName());
                        Object beanIns = clazz.getDeclaredConstructor().newInstance();
                        // 放入Map
                        ioc.put(beanName, beanIns);

                    } else if (clazz.isAnnotationPresent(SelfService.class)) {
                        // 1. 优先使用别名
                        SelfService service = clazz.getAnnotation(SelfService.class);
                        String beanName = service.value();
                        // 2. 没有则使用默认名称
                        if ("".equals(beanName.trim())) {
                            beanName = toLowerFirstCase(clazz.getSimpleName());
                        }
                        Object beanIns = clazz.getDeclaredConstructor().newInstance();
                        // 放入Map
                        ioc.put(beanName, beanIns);
                        // 3. 如果是接口, 只能初始化实现类
                        for (Class<?> anInterface : clazz.getInterfaces()) {
                            if (ioc.containsKey(anInterface.getName())) {
                                throw new Exception("你一个接口有多个实现, 我无法给你实例bean了, 用别名吧");
                            }
                            ioc.put(anInterface.getName(), beanIns);
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void doAutowired() {
        if (!ioc.isEmpty()) {
            // 对所有Bean都进行遍历
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                // 遍历获取其中需要Autowired的属性
                for (Field declaredField : entry.getValue().getClass().getDeclaredFields()) {
                    if (declaredField.isAnnotationPresent(SelfAutowired.class)) {
                        SelfAutowired autowired = declaredField.getAnnotation(SelfAutowired.class);
                        // 找别名, 没有别名就用类型作为名称
                        String beanName = autowired.value().trim();
                        if ("".equals(beanName)) {
                            beanName = declaredField.getType().getName();
                        }
                        // 属性赋值前, 暴力访问
                        declaredField.setAccessible(true);
                        try {
                            // 到ioc找到注册的bean进行赋值 (哪个对象, 赋什么值)
                            declaredField.set(entry.getValue(), ioc.get(beanName));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void doInitHandlerMapping() {
        if (!ioc.isEmpty()) {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                entry.getValue().getClass();
            }
        }
    }

    private void doDispatch() {

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
