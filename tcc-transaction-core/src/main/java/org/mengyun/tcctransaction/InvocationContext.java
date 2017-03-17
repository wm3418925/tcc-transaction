package org.mengyun.tcctransaction;

import java.io.Serializable;

/**
 * Created by changmingxie on 11/9/15.
 */
public class InvocationContext implements Serializable {

    private static final long serialVersionUID = -7969140711432461165L;
    private Class targetClass;
    private String targetResouceName;

    private String methodName;

    private Class[] parameterTypes;

    private Object[] args;

    public InvocationContext() {

    }

    public InvocationContext(Class targetClass, String targetResouceName, String methodName, Class[] parameterTypes, Object... args) {
        this.methodName = methodName;
        this.targetResouceName = targetResouceName;
        this.parameterTypes = parameterTypes;
        this.targetClass = targetClass;
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public String getTargetResouceName() {
        return targetResouceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }
}
