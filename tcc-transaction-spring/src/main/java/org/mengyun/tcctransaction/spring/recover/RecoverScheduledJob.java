package org.mengyun.tcctransaction.spring.recover;

import org.mengyun.tcctransaction.SystemException;
import org.mengyun.tcctransaction.recover.TransactionRecovery;
import org.mengyun.tcctransaction.support.TransactionConfigurator;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

/**
 * Created by changming.xie,wangmin on 6/2/16.
 */
public class RecoverScheduledJob {

    private TransactionRecovery transactionRecovery;

    private TransactionConfigurator transactionConfigurator;

    private Scheduler scheduler;

    public void init() {

        try {
            MethodInvokingJobDetailFactoryBean jobDetailFactory = new MethodInvokingJobDetailFactoryBean();
            jobDetailFactory.setTargetObject(transactionRecovery);
            jobDetailFactory.setTargetMethod("startRecover");
            jobDetailFactory.setName("transactionRecoveryJob");
            jobDetailFactory.setConcurrent(false);
            jobDetailFactory.afterPropertiesSet();

            // edit by wangmin
            CronTriggerFactoryBean cronTriggerFactory = new CronTriggerFactoryBean();
            cronTriggerFactory.setBeanName("transactionRecoveryCronTrigger");
            cronTriggerFactory.setCronExpression(transactionConfigurator.getRecoverConfig().getCronExpression());
            JobDetail jobDetail = new JobDetail();
            jobDetail.setName("transactionRecoveryJob");
            cronTriggerFactory.setJobDetail(jobDetail);
            cronTriggerFactory.afterPropertiesSet();
            CronTrigger cronTrigger = cronTriggerFactory.getObject();

            scheduler.scheduleJob(jobDetailFactory.getObject(), cronTrigger);

            scheduler.start();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void setTransactionRecovery(TransactionRecovery transactionRecovery) {
        this.transactionRecovery = transactionRecovery;
    }

    public void setTransactionConfigurator(TransactionConfigurator transactionConfigurator) {
        this.transactionConfigurator = transactionConfigurator;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
