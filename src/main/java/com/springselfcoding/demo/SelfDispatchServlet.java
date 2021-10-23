package com.springselfcoding.demo;

import com.springselfcoding.ioc.context.SelfApplicationContext;
import com.springselfcoding.mvcframework.SelfAutowired;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfRequestMapping;
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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class SelfDispatchServlet extends HttpServlet {

    // 加载配置
    private Properties contextConfig = new Properties();

    // 存放被扫描的类名
    private List<String> classNames = new ArrayList<>();

    // 存放实例
    private Map<String, Object> ioc = new HashMap<>();

    // 存放Url和Controller的处理方法, 进行绑定
    private Map<String, Method> handlerMapper = new HashMap<>();

    // 声明ApplicationContext, IoC容器的访问上下文
    private SelfApplicationContext applicationContext = null;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        applicationContext = new SelfApplicationContext(config.getInitParameter("contextConfigLocation"));

        // ------------- MVC ------------------
        // 5. 初始化HandlerMapping (url和method建立关系)
        doInitHandlerMapping();

        System.out.println("Self Dispatch Servlet finished initialization");

    }

    /**
     * 根据ContextConfigLocation这个名称, 去ClassPath下找到对应的配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:", ""));
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

    /**
     * 通过反射实例化Bean, 并且放入Ioc容器(Map)中
     */
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

    /**
     * 进行依赖注入
     */
    private void doAutowired() {
        if (!ioc.isEmpty()) {
            // 对所有Bean都进行遍历
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                // 遍历获取其中需要Autowired的属性
                for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SelfAutowired.class)) {
                        SelfAutowired autowired = field.getAnnotation(SelfAutowired.class);
                        // 找别名, 没有别名就用类型作为名称
                        String beanName = autowired.value().trim();
                        if ("".equals(beanName)) {
                            beanName = field.getType().getName();
                        }
                        // 属性赋值前, 暴力访问
                        field.setAccessible(true);
                        try {
                            // 到ioc找到注册的bean进行赋值 (哪个对象, 赋什么值)
                            field.set(entry.getValue(), ioc.get(beanName));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }
            }
        }
    }

    /**
     * 建立Url和Method的关联
     */
    private void doInitHandlerMapping() {
        if (!ioc.isEmpty()) {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
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
        method.invoke(ioc.get(beanName), new Object[]{req, resp, parameterMap.get("name")[0]});
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
