package org.mengyun.tcctransaction.support;

/**
 * Created by changmingxie on 11/20/15.
 */
public interface BeanFactory {
    <T> T getBean(String name, Class<T> var1);
    <T> T getBean(String name);
    <T> T getBean(Class<T> var1);

    String getBeanName(Object bean, Class targetClass);
}
