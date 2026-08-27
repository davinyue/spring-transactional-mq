package org.rdlinux.transactionalmq.starter.config;

import org.flywaydb.core.Flyway;
import org.rdlinux.ezmybatis.constant.DbType;
import org.springframework.beans.factory.InitializingBean;

/**
 * 事务消息 Flyway 迁移初始化器。
 */
public class TransactionalMqFlywayInitializer implements InitializingBean {

    private final Flyway flyway;

    /**
     * 创建事务消息 Flyway 迁移初始化器。
     *
     * @param flyway 事务消息专用 Flyway Bean
     */
    public TransactionalMqFlywayInitializer(Flyway flyway) {
        this.flyway = flyway;
    }

    /**
     * 在 Spring Bean 初始化后执行未完成的事务消息表迁移。
     */
    @Override
    public void afterPropertiesSet() {
        this.flyway.migrate();
    }

    /**
     * 根据数据库类型获取事务消息 Flyway 迁移目录。
     *
     * @param dbType ez-mybatis 数据库类型
     * @return classpath 迁移目录；不支持时返回 {@code null}
     */
    static String resolveScriptLocation(DbType dbType) {
        if (dbType == null) {
            return null;
        }
        switch (dbType) {
            case MYSQL:
                return "classpath:db/migration/mysql";
            case ORACLE:
                return "classpath:db/migration/oracle";
            case DM:
                return "classpath:db/migration/dm";
            case POSTGRE_SQL:
                return "classpath:db/migration/postgresql";
            case SQL_SERVER:
                return "classpath:db/migration/sqlserver";
            default:
                return null;
        }
    }
}
