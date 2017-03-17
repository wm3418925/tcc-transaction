package org.mengyun.tcctransaction.support;

/**
 * Created by changmingxie on 11/20/15.
 */
public interface BeanFactory {
    Object getBean(Class<?> aClass);
    Object getBean(String name);
    Object getBean(String name, Class<?> aClass);

    String getBeanName(Object bean, Class targetClass);
}
