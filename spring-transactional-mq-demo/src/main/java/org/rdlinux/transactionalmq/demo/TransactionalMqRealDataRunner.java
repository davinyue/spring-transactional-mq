package org.rdlinux.transactionalmq.demo;

import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 真实数据链路测试 runner
 */
@Component
@ConditionalOnProperty(prefix = "demo.transactional-mq", name = "run-on-startup", havingValue = "true")
public class TransactionalMqRealDataRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalMqRealDataRunner.class);

    private final MessagePublishService messagePublishService;
    private final TransactionalMqDemoProperties properties;

    /**
     * 构造真实数据链路测试 runner
     *
     * @param messagePublishService 消息发布服务
     * @param properties            demo 配置
     */
    public TransactionalMqRealDataRunner(MessagePublishService messagePublishService,
                                         TransactionalMqDemoProperties properties) {
        this.messagePublishService = messagePublishService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String messageKey = "demo-" + System.currentTimeMillis();
        TransactionalMessage<Map<String, Object>> message = new TransactionalMessage<Map<String, Object>>()
                .setMessageKey(messageKey)
                .setProducerCode("spring-transactional-mq-demo")
                .setDestination(this.properties.getDestination())
                .setBizKey(messageKey)
                .setPayload(this.buildPayload(messageKey));

        String messageId = this.messagePublishService.send(MqType.RABBITMQ, message);
        LOGGER.info("Transactional MQ real demo finished, id={}, messageKey={}, submitted=true",
                messageId, message.getMessageKey());
    }

    private Map<String, Object> buildPayload(String messageKey) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("messageKey", messageKey);
        payload.put("source", "spring-transactional-mq-demo");
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
