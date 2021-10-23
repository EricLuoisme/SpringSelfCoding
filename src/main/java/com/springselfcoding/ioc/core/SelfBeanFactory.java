package com.springselfcoding.ioc.core;

/**
 * 对象工厂顶层接口
 */
public interface SelfBeanFactory {

    Object getBean(Class beanClass);

    Object getBean(String beanName);
}
