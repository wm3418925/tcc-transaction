package org.mengyun.tcctransaction.support;

/**
 * Created by wm on 2017/3/17.
 */
public class FactoryBuilder {
    private static BeanFactory beanFactory;
    public static void setBeanFactory(BeanFactory beanFactory) {
        FactoryBuilder.beanFactory = beanFactory;
    }
    public static BeanFactory getBeanFactory() {
        return FactoryBuilder.beanFactory;
    }
}
