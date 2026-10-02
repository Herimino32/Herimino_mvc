package com.monframework.com.utils;

import java.lang.reflect.Method;

public class Mapping {
    private String className;
    private String method;

    public Mapping(String className, String method) {
        this.className = className;
        this.method = method;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public Object invoke() throws Exception {
        Class<?> clazz = Class.forName(this.className);

        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

        Method javaMethod = clazz.getDeclaredMethod(this.method);

        return javaMethod.invoke(controllerInstance);
    }
}
