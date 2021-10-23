package com.springselfcoding.ioc.context;

import com.springselfcoding.ioc.beans.SelfBeanWrapper;
import com.springselfcoding.ioc.beans.config.SelfBeanDefinition;
import com.springselfcoding.ioc.beans.support.SelfBeanDefinitionReader;
import com.springselfcoding.ioc.beans.support.SelfDefaultListableBeanFactory;
import com.springselfcoding.ioc.core.SelfBeanFactory;
import com.springselfcoding.mvcframework.SelfAutowired;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 这里为了方便直接写成实体类, Spring中这里还是abstract的
 */
public class SelfApplicationContext implements SelfBeanFactory {

    // 扫描并读入配置信息
    private SelfBeanDefinitionReader reader;

    // 缓存Bean信息的Factory
    public SelfDefaultListableBeanFactory registry = new SelfDefaultListableBeanFactory();

    /**
     * 查看是否有Bean
     */
    public int getBeanDefinitionCount() {
        return this.registry.beanDefinitionMap.size();
    }

    /**
     * 查看是否有该Bean
     */
    public String[] getBeanDefinitionNames() {
        return this.registry.beanDefinitionMap.keySet().toArray(new String[0]);
    }

    /**
     * 构造方法, 包含读取配置文件, 解析并封装为BeanDefinition对象, 缓存配置信息到BeanFactory中
     */
    public SelfApplicationContext(String... configurations) {
        try {
            // 1. 通过BeanDefinitionReader读取配置文件
            reader = new SelfBeanDefinitionReader(configurations);

            // 2. 解析配置信息, 将配置信息封装为BeanDefinition对象
            List<SelfBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();

            // 3. 缓存所有配置信息 (registry), 因为存在延迟加载的Bean, 实例化前先将它们存起来
            this.registry.doRegisterBeanDefinition(beanDefinitions);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // 4. 加载非延时加载的Bean, 实例化它们
        doLoadInstance();
    }

    @Override
    public Object getBean(Class beanClass) {
        return registry.getBean(beanClass);
    }

    @Override
    public Object getBean(String beanName) {
       return registry.getBean(beanName);
    }

    private void doLoadInstance() {
        // 循环调用BeanFactory的getBean方法, 对每个Bean进行实例化
        for (Map.Entry<String, SelfBeanDefinition> entry : this.registry.beanDefinitionMap.entrySet()) {
            if (!entry.getValue().isLazyInit()) {
                // 不是延时加载的才在这里加载, 延时加载的在getBean的时候才加载
                String beanName = entry.getKey();
                getBean(beanName);
            }
        }
    }

}
