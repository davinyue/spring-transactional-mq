package org.rdlinux.transactionalmq.starter.config;

import org.apache.ibatis.session.Configuration;
import org.rdlinux.ezmybatis.constant.DbType;
import org.rdlinux.ezmybatis.core.EzMybatisContent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 事务消息表结构初始化器
 */
public class TransactionalMqSchemaInitializer implements InitializingBean {

    private final DataSource dataSource;
    private final Configuration configuration;

    public TransactionalMqSchemaInitializer(DataSource dataSource, Configuration configuration) {
        this.dataSource = dataSource;
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() {
        try {
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

    protected void executeScript(String scriptLocation) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(scriptLocation));
        DatabasePopulatorUtils.execute(populator, this.dataSource);
    }

    static String resolveScriptLocation(DbType dbType) {
        if (dbType == null) {
            return null;
        }
        switch (dbType) {
            case MYSQL:
                return "sql/MYSQL.sql";
            case ORACLE:
                return "sql/ORACLE.sql";
            case DM:
                return "sql/DM.sql";
            case POSTGRE_SQL:
                return "sql/POSTGRE_SQL.sql";
            case SQL_SERVER:
                return "sql/SQL_SERVER.sql";
            default:
                return null;
        }
    }
}
