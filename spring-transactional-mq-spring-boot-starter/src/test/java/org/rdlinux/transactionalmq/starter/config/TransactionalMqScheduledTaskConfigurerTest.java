package org.rdlinux.transactionalmq.starter.config;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.core.service.ConsumedMessageCleanupScheduler;
import org.rdlinux.transactionalmq.core.service.TransactionalMessageCleanupScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * 定时任务编码注册器测试
 */
public class TransactionalMqScheduledTaskConfigurerTest {

    @Test
    public void configureTasksShouldRegisterCleanupCronTasks() {
        TransactionalMqProperties properties = new TransactionalMqProperties();
        properties.setConsumeRecordCleanupCron("0 1 2 * * ?");
        properties.setSuccessMessageCleanupCron("0 2 3 * * ?");
        TransactionalMqScheduledTaskConfigurer configurer = new TransactionalMqScheduledTaskConfigurer(
                mock(ConsumedMessageCleanupScheduler.class),
                mock(TransactionalMessageCleanupScheduler.class),
                properties);
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

        configurer.configureTasks(taskRegistrar);

        List<CronTask> cronTasks = taskRegistrar.getCronTaskList();
        Assert.assertEquals(2, cronTasks.size());
        Assert.assertEquals("0 1 2 * * ?", cronTasks.get(0).getExpression());
        Assert.assertEquals("0 2 3 * * ?", cronTasks.get(1).getExpression());
    }
}
