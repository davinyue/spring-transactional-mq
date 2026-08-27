package org.rdlinux.transactionalmq.starter.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.rdlinux.ezmybatis.constant.DbType;
import org.rdlinux.ezmybatis.core.EzMybatisContent;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 事务消息 Flyway 迁移初始化器。
 */
public class TransactionalMqFlywayInitializer implements InitializingBean {

    /**
     * 数据源。
     */
    private final DataSource dataSource;

    /**
     * MyBatis 配置。
     */
    private final MybatisProperties mybatisProperties;

    /**
     * 事务消息配置。
     */
    private final TransactionalMqProperties properties;

    /**
     * 创建事务消息 Flyway 迁移初始化器。
     *
     * @param dataSource        数据源
     * @param mybatisProperties MyBatis 配置
     * @param properties        事务消息配置
     */
    public TransactionalMqFlywayInitializer(DataSource dataSource, MybatisProperties mybatisProperties,
                                            TransactionalMqProperties properties) {
        this.dataSource = dataSource;
        this.mybatisProperties = mybatisProperties;
        this.properties = properties;
    }

    /**
     * 在 Spring Bean 初始化后执行未完成的事务消息表迁移。
     */
    @Override
    public void afterPropertiesSet() {
        DbType dbType = EzMybatisContent.getDbType(this.mybatisProperties.getConfiguration());
        String scriptLocation = resolveScriptLocation(dbType);
        if (scriptLocation == null) {
            throw new IllegalStateException("Unsupported transactional message database type: " + dbType);
        }
        if (!StringUtils.hasText(this.properties.getSchemaHistoryTable())) {
            throw new IllegalStateException("Transactional message Flyway schema history table must not be empty");
        }
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(this.dataSource)
                .locations(scriptLocation)
                .table(this.properties.getSchemaHistoryTable());
        if (StringUtils.hasText(this.properties.getSchemaBaselineVersion())) {
            configuration.baselineOnMigrate(true).baselineVersion(this.properties.getSchemaBaselineVersion());
        }
        configuration.load().migrate();
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
                return "classpath:transactionalmq/db/migration/mysql";
            case ORACLE:
                return "classpath:transactionalmq/db/migration/oracle";
            case DM:
                return "classpath:transactionalmq/db/migration/dm";
            case POSTGRE_SQL:
                return "classpath:transactionalmq/db/migration/postgresql";
            case SQL_SERVER:
                return "classpath:transactionalmq/db/migration/sqlserver";
            default:
                return null;
        }
    }
}
