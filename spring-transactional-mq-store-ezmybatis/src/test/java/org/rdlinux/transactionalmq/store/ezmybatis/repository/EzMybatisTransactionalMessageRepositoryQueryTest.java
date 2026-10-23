package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.mapper.EzMapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * EzMybatis 事务消息仓储查询测试
 */
public class EzMybatisTransactionalMessageRepositoryQueryTest {

    /**
     * 验证最近派发时间查询使用 Date 标量结果并限制为一行。
     *
     * @throws Exception 注入 EzDao 或读取查询对象失败
     */
    @Test
    public void findNextDispatchTimeShouldUseDateScalarAndLimitOne() throws Exception {
        Date expectedTime = new Date(System.currentTimeMillis() + 5000L);
        AtomicReference<EzQuery<?>> capturedQuery = new AtomicReference<EzQuery<?>>();
        EzMybatisTransactionalMessageRepository repository = new EzMybatisTransactionalMessageRepository();
        Field ezDaoField = EzMybatisTransactionalMessageRepository.class.getDeclaredField("ezDao");
        ezDaoField.setAccessible(true);
        ezDaoField.set(repository, new EzDao(this.buildCapturingMapper(capturedQuery, expectedTime)));

        Date actualTime = repository.findNextDispatchTime();

        Assert.assertSame(expectedTime, actualTime);
        Assert.assertNotNull(capturedQuery.get());
        Assert.assertEquals(Date.class, capturedQuery.get().getRetType());
        Assert.assertEquals(1, capturedQuery.get().getSelect().getSelectItems().size());
        Assert.assertEquals(1, capturedQuery.get().getPage().getSize());
        Assert.assertEquals(0, capturedQuery.get().getPage().getSkip());
    }

    /**
     * 构建捕获 queryOne 参数并返回指定日期的 EzMapper 代理。
     *
     * @param capturedQuery 捕获查询对象的引用
     * @param expectedTime  查询返回时间
     * @return EzMapper 代理
     */
    private EzMapper buildCapturingMapper(AtomicReference<EzQuery<?>> capturedQuery, Date expectedTime) {
        return (EzMapper) Proxy.newProxyInstance(EzMapper.class.getClassLoader(), new Class<?>[]{EzMapper.class},
                new InvocationHandler() {
                    /**
                     * 捕获仓储生成的查询对象并返回预设时间。
                     *
                     * @param proxy 代理对象
                     * @param method 被调用的方法
                     * @param args 方法参数
                     * @return 查询结果
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("queryOne".equals(method.getName())) {
                            capturedQuery.set((EzQuery<?>) args[0]);
                            return expectedTime;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}
