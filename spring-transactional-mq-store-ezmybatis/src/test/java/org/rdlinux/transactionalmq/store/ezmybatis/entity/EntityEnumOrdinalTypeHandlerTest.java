package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.lang.reflect.Field;

import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.ezmybatis.annotation.TypeHandler;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;

public class EntityEnumOrdinalTypeHandlerTest {

    @Test
    public void transactionalMessageEntityEnumFieldsShouldUseOrdinalTypeHandler() throws Exception {
        assertOrdinalTypeHandler(TransactionalMessageEntity.class, "mqType", MqType.class);
        assertOrdinalTypeHandler(TransactionalMessageEntity.class, "messageStatus", MessageStatus.class);
    }

    @Test
    public void consumedMessageEntityEnumFieldsShouldUseOrdinalTypeHandler() throws Exception {
        assertOrdinalTypeHandler(ConsumedMessageEntity.class, "consumeStatus", ConsumeStatus.class);
        assertOrdinalTypeHandler(ConsumedMessageHistoryEntity.class, "consumeStatus", ConsumeStatus.class);
    }

    @Test
    public void messageSendLogEntityEnumFieldsShouldUseOrdinalTypeHandler() throws Exception {
        assertOrdinalTypeHandler(MessageSendLogEntity.class, "mqType", MqType.class);
        assertOrdinalTypeHandler(MessageSendLogEntity.class, "sendStatus", SendStatus.class);
    }

    private void assertOrdinalTypeHandler(Class<?> entityClass, String fieldName, Class<?> fieldType) throws Exception {
        Field field = entityClass.getDeclaredField(fieldName);

        Assert.assertEquals(fieldType, field.getType());
        TypeHandler typeHandler = field.getAnnotation(TypeHandler.class);
        Assert.assertNotNull(typeHandler);
        Assert.assertEquals(EnumOrdinalTypeHandler.class, typeHandler.value());
    }
}
