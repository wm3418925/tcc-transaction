package org.mengyun.tcctransaction.interceptor;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.mengyun.tcctransaction.*;
import org.mengyun.tcctransaction.api.*;
import org.mengyun.tcctransaction.support.FactoryBuilder;
import org.mengyun.tcctransaction.utils.CompensableMethodUtils;
import org.mengyun.tcctransaction.utils.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

/**
 * Created by changmingxie on 11/8/15.
 */
public class ResourceCoordinatorInterceptor {
    static final Logger logger = Logger.getLogger(ResourceCoordinatorInterceptor.class.getSimpleName());

    private TransactionManager transactionManager;


    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public Object interceptTransactionContextMethod(ProceedingJoinPoint pjp) throws Throwable {

        Transaction transaction = transactionManager.getCurrentTransaction();

        if (transaction != null) {

            switch (transaction.getStatus()) {
                case TRYING:
                    enlistParticipant(pjp);
                    break;
                case CONFIRMING:
                    break;
                case CANCELLING:
                    break;
            }
        }

        if (transaction != null)
            return proceedAndCheckArgs(pjp);
        else
            return pjp.proceed(pjp.getArgs());
    }

    private void enlistParticipant(ProceedingJoinPoint pjp) throws IllegalAccessException, InstantiationException {

        Method method = CompensableMethodUtils.getCompensableMethod(pjp);
        Compensable compensable = method.getAnnotation(Compensable.class);

        String confirmMethodName = compensable.confirmMethod();
        String cancelMethodName = compensable.cancelMethod();

        Transaction transaction = transactionManager.getCurrentTransaction();
        TransactionXid xid = new TransactionXid(transaction.getXid().getGlobalTransactionId());

        TransactionContextEditor transactionContextEditor = FactoryBuilder.getBeanFactory().getBean(compensable.transactionContextEditor());
        if (transactionContextEditor.get(pjp.getTarget(), method, pjp.getArgs()) == null) {
            transactionContextEditor.set(new TransactionContext(xid, TransactionStatus.TRYING.getId()), pjp.getTarget(), ((MethodSignature) pjp.getSignature()).getMethod(), pjp.getArgs());
        }

        Class targetClass = ReflectionUtils.getDeclaringType(pjp.getTarget().getClass(), method.getName(), method.getParameterTypes());
        String beanName = FactoryBuilder.getBeanFactory().getBeanName(pjp.getTarget(), targetClass);

        InvocationContext confirmInvocation = new InvocationContext(
                targetClass, beanName,
                confirmMethodName,
                method.getParameterTypes(), pjp.getArgs());

        InvocationContext cancelInvocation = new InvocationContext(
                targetClass, beanName,
                cancelMethodName,
                method.getParameterTypes(), pjp.getArgs());

        Participant participant =
                new Participant(
                        xid,
                        confirmInvocation,
                        cancelInvocation,
                        compensable.transactionContextEditor());

        transactionManager.enlistParticipant(participant);
    }


    /**
     * 在执行方法后, 参数被修改, 则调用 flushTransactionToRepository
     * */
    private Object proceedAndCheckArgs(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (null == args || args.length <= 0) {
            return pjp.proceed(args);
        }


        // 1 执行被切面方法前, 对参数序列化, 用来判断参数是否变化
        byte[] beforeObjectBytes = serializeObject(args);

        // 2 执行被切面方法, 并捕获异常. 异常会在最后抛出
        Object result = null;
        Throwable throwableObj = null;
        try {
            result = pjp.proceed(args);
        } catch (Throwable e) {
            throwableObj = e;
        }

        // 3 判断参数是否被修改, 如果被修改则保存 Transaction
        if (null == beforeObjectBytes) {
            flushTransactionToRepository();
        } else {
            byte[] afterObjectBytes = serializeObject(args);
            if (null == afterObjectBytes || !isByteArrayEqual(beforeObjectBytes, afterObjectBytes)) {
                flushTransactionToRepository();
            }
        }

        // 4 被切面方法如果抛出异常, 在这里抛出, 否则返回执行结果
        if (throwableObj != null)
            throw throwableObj;
        else
            return result;
    }
    /**
     * 强制将 Transaction 存入 Repository
     * */
    private boolean flushTransactionToRepository() {
        try {
            Transaction transaction = transactionManager.getCurrentTransaction();
            if (null == transaction)
                return false;

            transactionManager.updateTransaction(transaction);
            return true;
        } catch (Throwable e) {
            logger.info(e);
            return false;
        }
    }

    /**
     * 将对象序列化为byte数组
     * */
    private static byte[] serializeObject(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);

            byte[] ba = baos.toByteArray();

            baos.close();
            oos.close();

            return ba;
        } catch (Throwable e) {
            logger.debug("", e);
            return null;
        }
    }
    /**
     * 判断byte数组是否相等
     * */
    private static boolean isByteArrayEqual(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;
        for (int i=0; i<a.length; ++i) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }
}
