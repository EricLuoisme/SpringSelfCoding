package com.springselfcoding.ioc.beans.support;


import com.springselfcoding.ioc.beans.config.SelfBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SelfBeanDefinitionReader {

    // 加载配置
    private Properties contextConfig = new Properties();

    // 存放被扫描的全类名, 需要被注册的Bean class
    private List<String> registryBeanClass = new ArrayList<>();

    /**
     * 构造方法, 包含加载配置信息, 并扫描类
     */
    public SelfBeanDefinitionReader(String... locations) {
        // 1. 加载Properties文件, 直接用原SelfDispatchServlet的即可
        doLoadConfig(locations[0]);

        // 2. 扫描路径下相关类
        doScanner(contextConfig.getProperty("scanPackage"));
    }

    /**
     * 解析配置信息, 并封装为BeanDefinition
     */
    public List<SelfBeanDefinition> loadBeanDefinitions() {
        List<SelfBeanDefinition> resultList = new ArrayList<>();
        try {
            // 遍历前面扫描到的class
            for (String beanClass : registryBeanClass) {
                Class<?> clazz = Class.forName(beanClass);

                // 本身是接口, 直接过滤
                if (clazz.isInterface()) {
                    continue;
                }

                // 1. 默认类名首字母小写的情况
                resultList.add(doCreateBeanDefinition(toLowerFirstCase(clazz.getSimpleName()), clazz.getName()));

                // 2. 如果是接口的实现, 就用接口名为beanName
                for (Class<?> anInterface : clazz.getInterfaces()) {
                    resultList.add(doCreateBeanDefinition(anInterface.getName(), clazz.getName()));
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 根据ContextConfigLocation这个名称, 去ClassPath下找到对应的配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(
                contextConfigLocation.replaceAll("classpath:", ""));
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
                registryBeanClass.add(className);
            }
            // 不是class结尾的不进行处理
        }
    }

    /**
     * 为一个Bean封装其BeanDefinition
     */
    private SelfBeanDefinition doCreateBeanDefinition(String factoryBeanName, String factoryClassName) {
        SelfBeanDefinition beanDefinition = new SelfBeanDefinition();
        beanDefinition.setFactoryBeanName(factoryBeanName);
        beanDefinition.setBeanClassName(factoryClassName);
        return beanDefinition;
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
