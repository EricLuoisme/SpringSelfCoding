package com.springselfcoding.ioc.beans;

public class SelfBeanWrapper {

    private Object wrappedInstance;
    private Class<?> wrappedClass;

    public SelfBeanWrapper(Object wrappedInstance) {
        this.wrappedInstance = wrappedInstance;
        this.wrappedClass = wrappedInstance.getClass();
    }

    public Object getWrappedInstance() {
        return this.wrappedInstance;
    }

    public Class<?> getWrappedClass() {
        return this.wrappedClass;
    }

}
