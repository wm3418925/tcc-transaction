package org.mengyun.tcctransaction;

import org.mengyun.tcctransaction.api.TransactionContext;
import org.mengyun.tcctransaction.api.TransactionContextEditor;
import org.mengyun.tcctransaction.support.FactoryBuilder;
import org.mengyun.tcctransaction.utils.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by changmingxie on 10/30/15.
 */
public class Terminator implements Serializable {

    private static final long serialVersionUID = -164958655471605778L;


    public Terminator() {
    }

    public Object invoke(TransactionContext transactionContext, InvocationContext invocationContext, Class<? extends TransactionContextEditor> transactionContextEditorClass) {
        if (StringUtils.isNotEmpty(invocationContext.getMethodName())) {
            try {
                Object target;
                if (StringUtils.isNotEmpty(invocationContext.getTargetResouceName()) && invocationContext.getTargetClass() != null) {
                    target = FactoryBuilder.getBeanFactory().getBean(invocationContext.getTargetResouceName(), invocationContext.getTargetClass());
                } else if (StringUtils.isNotEmpty(invocationContext.getTargetResouceName())) {
                    target = FactoryBuilder.getBeanFactory().getBean(invocationContext.getTargetResouceName());
                } else if (invocationContext.getTargetClass() != null) {
                    target = FactoryBuilder.getBeanFactory().getBean(invocationContext.getTargetClass());
                } else {
                    return null;
                }

                Method method = target.getClass().getMethod(invocationContext.getMethodName(), invocationContext.getParameterTypes());

                FactoryBuilder.getBeanFactory().getBean(transactionContextEditorClass).set(transactionContext, target, method, invocationContext.getArgs());

                return method.invoke(target, invocationContext.getArgs());
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return null;
    }
}
