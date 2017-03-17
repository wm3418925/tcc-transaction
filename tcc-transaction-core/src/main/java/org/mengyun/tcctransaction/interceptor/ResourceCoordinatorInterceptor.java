package org.mengyun.tcctransaction.interceptor;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.mengyun.tcctransaction.*;
import org.mengyun.tcctransaction.Terminator;
import org.mengyun.tcctransaction.api.TransactionContext;
import org.mengyun.tcctransaction.api.TransactionStatus;
import org.mengyun.tcctransaction.api.TransactionXid;
import org.mengyun.tcctransaction.common.MethodType;
import org.mengyun.tcctransaction.support.BeanFactoryAdapter;
import org.mengyun.tcctransaction.support.TransactionConfigurator;
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

    private TransactionConfigurator transactionConfigurator;

    public void setTransactionConfigurator(TransactionConfigurator transactionConfigurator) {
        this.transactionConfigurator = transactionConfigurator;
    }

    public Object interceptTransactionContextMethod(ProceedingJoinPoint pjp) throws Throwable {

        Transaction transaction = transactionConfigurator.getTransactionManager().getCurrentTransaction();

        if (transaction != null && transaction.getStatus().equals(TransactionStatus.TRYING)) {

            TransactionContext transactionContext = CompensableMethodUtils.getTransactionContextFromArgs(pjp.getArgs());

            Compensable compensable = getCompensable(pjp);

            MethodType methodType = CompensableMethodUtils.calculateMethodType(transactionContext, compensable != null ? true : false);

            switch (methodType) {
                case ROOT:
                    generateAndEnlistRootParticipant(pjp);
                    break;
                case CONSUMER:
                    generateAndEnlistConsumerParticipant(pjp);
                    break;
                case PROVIDER:
                    generateAndEnlistProviderParticipant(pjp);
                    break;
            }
        }

        if (transaction != null)
            return proceedAndCheckArgs(pjp);
        else
            return pjp.proceed(pjp.getArgs());
    }

    private Participant generateAndEnlistRootParticipant(ProceedingJoinPoint pjp) {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Compensable compensable = getCompensable(pjp);
        String confirmMethodName = compensable.confirmMethod();
        String cancelMethodName = compensable.cancelMethod();

        Transaction transaction = transactionConfigurator.getTransactionManager().getCurrentTransaction();

        TransactionXid xid = new TransactionXid(transaction.getXid().getGlobalTransactionId());

        int position = CompensableMethodUtils.getTransactionContextParamPosition(((MethodSignature) pjp.getSignature()).getParameterTypes());
        if (position >= 0) {
            pjp.getArgs()[position] = new TransactionContext(xid, transaction.getStatus().getId());
        }

        Class targetClass = ReflectionUtils.getDeclaringType(pjp.getTarget().getClass(), method.getName(), method.getParameterTypes());
        String beanName = BeanFactoryAdapter.getBeanName(pjp.getTarget(), targetClass);

        InvocationContext confirmInvocation = new InvocationContext(targetClass, beanName,
                confirmMethodName,
                method.getParameterTypes(), pjp.getArgs());
        InvocationContext cancelInvocation = new InvocationContext(targetClass, beanName,
                cancelMethodName,
                method.getParameterTypes(), pjp.getArgs());

        Participant participant =
                new Participant(
                        xid,
                        new Terminator(confirmInvocation, cancelInvocation));

        transaction.enlistParticipant(participant);

        TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
        transactionRepository.update(transaction);

        return participant;
    }

    private Participant generateAndEnlistConsumerParticipant(ProceedingJoinPoint pjp) {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        Transaction transaction = transactionConfigurator.getTransactionManager().getCurrentTransaction();

        TransactionXid xid = new TransactionXid(transaction.getXid().getGlobalTransactionId());

        int position = CompensableMethodUtils.getTransactionContextParamPosition(((MethodSignature) pjp.getSignature()).getParameterTypes());

        pjp.getArgs()[position] = new TransactionContext(xid, transaction.getStatus().getId());

        Object[] tryArgs = pjp.getArgs();
        Object[] confirmArgs = new Object[tryArgs.length];
        Object[] cancelArgs = new Object[tryArgs.length];

        System.arraycopy(tryArgs, 0, confirmArgs, 0, tryArgs.length);
        confirmArgs[position] = new TransactionContext(xid, TransactionStatus.CONFIRMING.getId());

        System.arraycopy(tryArgs, 0, cancelArgs, 0, tryArgs.length);
        cancelArgs[position] = new TransactionContext(xid, TransactionStatus.CANCELLING.getId());

        Class targetClass = ReflectionUtils.getDeclaringType(pjp.getTarget().getClass(), method.getName(), method.getParameterTypes());
        String beanName = BeanFactoryAdapter.getBeanName(pjp.getTarget(), targetClass);

        InvocationContext confirmInvocation = new InvocationContext(targetClass, beanName, method.getName(), method.getParameterTypes(), confirmArgs);
        InvocationContext cancelInvocation = new InvocationContext(targetClass, beanName, method.getName(), method.getParameterTypes(), cancelArgs);

        Participant participant =
                new Participant(
                        xid,
                        new Terminator(confirmInvocation, cancelInvocation));

        transaction.enlistParticipant(participant);

        TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();

        transactionRepository.update(transaction);

        return participant;
    }

    private Participant generateAndEnlistProviderParticipant(ProceedingJoinPoint pjp) {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        Compensable compensable = getCompensable(pjp);

        String confirmMethodName = compensable.confirmMethod();
        String cancelMethodName = compensable.cancelMethod();

        Transaction transaction = transactionConfigurator.getTransactionManager().getCurrentTransaction();

        TransactionXid xid = new TransactionXid(transaction.getXid().getGlobalTransactionId());


        Class targetClass = ReflectionUtils.getDeclaringType(pjp.getTarget().getClass(), method.getName(), method.getParameterTypes());
        String beanName = BeanFactoryAdapter.getBeanName(pjp.getTarget(), targetClass);

        InvocationContext confirmInvocation = new InvocationContext(targetClass, beanName,
                confirmMethodName,
                method.getParameterTypes(), pjp.getArgs());
        InvocationContext cancelInvocation = new InvocationContext(targetClass, beanName,
                cancelMethodName,
                method.getParameterTypes(), pjp.getArgs());

        Participant participant =
                new Participant(
                        xid,
                        new Terminator(confirmInvocation, cancelInvocation));

        transaction.enlistParticipant(participant);

        TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
        transactionRepository.update(transaction);

        return participant;
    }

    private Compensable getCompensable(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        Compensable compensable = method.getAnnotation(Compensable.class);

        if (compensable == null) {
            Method targetMethod = null;
            try {
                targetMethod = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());

                if (targetMethod != null) {
                    compensable = targetMethod.getAnnotation(Compensable.class);
                }

            } catch (NoSuchMethodException e) {
                compensable = null;
            }

        }
        return compensable;
    }


    /**
     * 在执行方法后, 参数被修改, 则调用 flushTransactionToRepository
     * added by wangmin
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
     * added by wangmin
     * */
    private boolean flushTransactionToRepository() {
        try {
            Transaction transaction = transactionConfigurator.getTransactionManager().getCurrentTransaction();
            if (null == transaction)
                return false;

            TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
            transactionRepository.update(transaction);
            return true;
        } catch (Throwable e) {
            logger.info(e);
            return false;
        }
    }

    /**
     * 将对象序列化为byte数组
     * added by wangmin
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
     * added by wangmin
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
