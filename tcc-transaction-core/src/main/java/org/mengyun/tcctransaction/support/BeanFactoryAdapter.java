package org.mengyun.tcctransaction.support;

/**
 * Created by changmingxie on 11/20/15.
 */
public class BeanFactoryAdapter {

    private static BeanFactory beanFactory;

    public static Object getBean(Class<?> aClass) {
        return beanFactory.getBean(aClass);
    }
    public static Object getBean(String name) {
        return beanFactory.getBean(name);
    }
    public static Object getBean(String name, Class<?> aClass) {
        return beanFactory.getBean(name, aClass);
    }

    public static String getBeanName(Object bean, Class targetClass) {
        return beanFactory.getBeanName(bean, targetClass);
    }

    public static void setBeanFactory(BeanFactory beanFactory) {
        BeanFactoryAdapter.beanFactory = beanFactory;
    }
}
