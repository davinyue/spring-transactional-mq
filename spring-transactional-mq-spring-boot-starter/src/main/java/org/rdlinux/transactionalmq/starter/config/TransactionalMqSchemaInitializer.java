package org.rdlinux.transactionalmq.starter.config;

import org.apache.ibatis.session.Configuration;
import org.rdlinux.ezmybatis.constant.DbType;
import org.rdlinux.ezmybatis.core.EzMybatisContent;
import org.rdlinux.ezmybatis.core.mapper.EzMapper;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageHistoryEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.MessageSendLogEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageHistoryEntity;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * 事务消息表结构初始化器
 */
public class TransactionalMqSchemaInitializer implements InitializingBean {

    private static final List<Class<?>> TABLE_ENTITY_TYPES = Arrays.asList(
            TransactionalMessageEntity.class,
            TransactionalMessageHistoryEntity.class,
            ConsumedMessageEntity.class,
            ConsumedMessageHistoryEntity.class,
            MessageSendLogEntity.class);

    private final DataSource dataSource;
    private final Configuration configuration;
    private final EzMapper ezMapper;

    public TransactionalMqSchemaInitializer(DataSource dataSource, Configuration configuration, EzMapper ezMapper) {
        this.dataSource = dataSource;
        this.configuration = configuration;
        this.ezMapper = ezMapper;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            if (this.allTablesExist()) {
                return;
            }
            String scriptLocation = resolveScriptLocation(this.resolveDbType());
            if (scriptLocation == null) {
                return;
            }
            this.executeScript(scriptLocation);
        } catch (Exception ex) {
            // 按需求直接吞掉异常，不影响启动
        }
    }

    protected DbType resolveDbType() {
        return EzMybatisContent.getDbType(this.configuration);
    }

    protected boolean allTablesExist() {
        for (Class<?> entityType : TABLE_ENTITY_TYPES) {
            if (!this.ezMapper.tableExists(EntityTable.of(entityType))) {
                return false;
            }
        }
        return true;
    }

    protected void executeScript(String scriptLocation) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(scriptLocation));
        DatabasePopulatorUtils.execute(populator, this.dataSource);
    }

    static String resolveScriptLocation(DbType dbType) {
        if (dbType == null) {
            return null;
        }
        return "sql/" + dbType.name() + ".sql";
    }
}
