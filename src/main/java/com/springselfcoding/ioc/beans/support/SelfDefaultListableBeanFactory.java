package com.springselfcoding.ioc.beans.support;

import com.springselfcoding.ioc.beans.SelfBeanWrapper;
import com.springselfcoding.ioc.beans.config.SelfBeanDefinition;
import com.springselfcoding.ioc.core.SelfBeanFactory;
import com.springselfcoding.mvcframework.SelfAutowired;
import com.springselfcoding.mvcframework.SelfController;
import com.springselfcoding.mvcframework.SelfService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描完Bean并都Wrapped为BeanDefinition后, 使用Factory进行Bean的实例化
 * 1) 三级缓存, 第一级就是beanDefinitionMap
 * 2) 第二级缓存, 通过反射实例化bean之后(Singleton), 会将其存入factoryBeanObjCache, 后面可能会需要AOP
 * 3) 实例化后的bean, 通过wrapper为统一好管理的实例, 通过反射找到需要依赖注入的属性(或者是constructor的), 会从factoryBeanInstanceCache中找
 * 4) 实例化并依赖注入完毕后, 放入factoryBeanInstanceCache
 */
public class SelfDefaultListableBeanFactory implements SelfBeanFactory {

    // 保存所有bean的配置信息
    public Map<String, SelfBeanDefinition> beanDefinitionMap = new HashMap<>();

    // 三级缓存, 直接获取实例 (为了方便直接public出去了)
    public Map<String, SelfBeanWrapper> factoryBeanInstanceCache = new HashMap<>();

    // 保存直接的实例, 而不是Wrapper过的
    private Map<String, Object> factoryBeanObjCache = new HashMap<>();


    @Override
    public Object getBean(Class beanClass) {
        return this.getBean(beanClass.getName());
    }

    @Override
    public Object getBean(String beanName) {
        // 1. 获取配置信息
        SelfBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);

        // 2. 反射实例化对象
        Object ins = instantiateBean(beanName, beanDefinition);

        // 3. 将Bean实例转换为BeanWrapper方便管理
        SelfBeanWrapper beanWrapper = new SelfBeanWrapper(ins);

        // 4. 依赖注入
        injection(beanName, beanDefinition, beanWrapper);

        // 5. 将Bean实例的Wrapper版, 缓存到IoC容器中
        this.factoryBeanInstanceCache.put(beanName, beanWrapper);

        return beanWrapper;
    }

    /**
     * 将beanDefinitions遍历, 注册到Map中
     */
    public void doRegisterBeanDefinition(List<SelfBeanDefinition> beanDefinitions) throws Exception {
        for (SelfBeanDefinition beanDefinition : beanDefinitions) {
            if (this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception(beanDefinition.getFactoryBeanName() + " is repeated");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
        }
    }

    /**
     * 根据BeanDefinition实例化Bean对象
     */
    private Object instantiateBean(String beanName, SelfBeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object ins = null;
        try {
            Class<?> clazz = Class.forName(className);

            ins = clazz.getDeclaredConstructor().newInstance();

            /**
             * 如果是代理对象, 会触发AOP逻辑
             */

            // 以防代理时, 丢失了原生对象
            this.factoryBeanObjCache.put(beanName, ins);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ins;
    }

    /**
     * 对实例化后的Bean进行依赖注入
     */
    private void injection(String beanName, SelfBeanDefinition beanDefinition, SelfBeanWrapper beanWrapper) {

        Object instance = beanWrapper.getWrappedInstance();

        Class<?> clazz = beanWrapper.getWrappedClass();

        // 不是被定义的注解直接返回
        if (!clazz.isAnnotationPresent(SelfController.class) && !clazz.isAnnotationPresent(SelfService.class)) {
            return;
        }

        // 遍历获取其中需要Autowired的属性
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(SelfAutowired.class)) {
                SelfAutowired autowired = field.getAnnotation(SelfAutowired.class);
                // 找别名, 没有别名就用类型作为名称
                String autowiredBeanName = autowired.value().trim();
                if ("".equals(autowiredBeanName)) {
                    autowiredBeanName = field.getType().getName();
                }
                // 属性赋值前, 暴力访问
                field.setAccessible(true);
                try {
                    SelfBeanWrapper autowiredBeanWrapper = factoryBeanInstanceCache.get(autowiredBeanName);
                    if (null == autowiredBeanWrapper) {
                        return;
                    }
                    // 到ioc找到注册的bean进行赋值 (哪个对象, 赋什么值)
                    field.set(instance, autowiredBeanWrapper.getWrappedInstance());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    // 某个报错不能影响继续加载
                    continue;
                }
            }
        }
    }
}
