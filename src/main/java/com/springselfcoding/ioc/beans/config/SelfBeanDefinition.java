package com.springselfcoding.ioc.beans.config;

/**
 * BeanDefinition的模拟, 用实例替代抽象
 */
public class SelfBeanDefinition {

    private String factoryBeanName;

    private String beanClassName;


    public boolean isLazyInit() {
        return false;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }
}
