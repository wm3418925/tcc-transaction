package org.mengyun.tcctransaction.spring.support;

import org.mengyun.tcctransaction.support.BeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by changmingxie on 11/22/15.
 */
public class SpringBeanFactory implements BeanFactory, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public <T> T getBean(Class<T> clazz) {
        return this.applicationContext.getBean(clazz);
    }
    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        return this.applicationContext.getBean(name, clazz);
    }
    @Override
    public <T> T getBean(String name) {
        return (T) this.applicationContext.getBean(name);
    }

    @Override
    public String getBeanName(Object bean, Class targetClass) {
        try {
            Map<String, Object> beans = applicationContext.getBeansOfType(targetClass);
            if (null == beans)
                return null;

            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                Object fb = entry.getValue();
                if (isBeanEqual(bean, fb)) {
                    return entry.getKey();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private static boolean isBeanEqual(Object a, Object b) {
        if (a == b)
            return true;
        return getBeanTarget(a) == getBeanTarget(b);
    }
    private static Object getBeanTarget(Object bean) {
        try {
            Field callback1Field = bean.getClass().getDeclaredField("CGLIB$CALLBACK_1");
            if (null == callback1Field)
                return bean;
            callback1Field.setAccessible(true);
            Object callback1 = callback1Field.get(bean);
            if (null == callback1)
                return bean;

            Field targetField = callback1.getClass().getDeclaredField("target");
            if (null == targetField)
                return bean;
            targetField.setAccessible(true);
            Object target = targetField.get(callback1);
            if (null != target)
                return target;

            return bean;
        } catch (Exception e) {
            return bean;
        }
    }
}
