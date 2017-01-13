package com.realtek.DLNA_DMP_1p5;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DLNACallback {
    private String methodName;
    private Object scope;
    
    public DLNACallback(Object scope, String methodName)
    {
        this.methodName = methodName;
        this.scope = scope;
    }

    public Object invoke(Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        Method method = scope.getClass().getMethod(methodName, getParameterClasses(parameters));
        return method.invoke(scope, parameters);
    }

    private Class[] getParameterClasses(Object... parameters)
    {
        Class[] classes = new Class[parameters.length];
        for (int i=0; i < classes.length; i++)
        {
            classes[i] = parameters[i].getClass();
        }
        return classes;
    }
}
