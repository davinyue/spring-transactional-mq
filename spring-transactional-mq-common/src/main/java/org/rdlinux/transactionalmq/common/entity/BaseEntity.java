package org.rdlinux.transactionalmq.common.entity;

import java.util.Date;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;

/**
 * 公共基础实体。
 *
 * @param <T> 子类类型
 */
@Getter
@FieldNameConstants
public abstract class BaseEntity<T extends BaseEntity<T>> {

    /**
     * 主键标识。
     */
    private String id;
    /**
     * 创建时间。
     */
    private Date createTime;
    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 设置主键标识。
     *
     * @param id 主键标识
     * @return 子类自身
     */
    public T setId(String id) {
        this.id = id;
        return castSelf();
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     * @return 子类自身
     */
    public T setCreateTime(Date createTime) {
        this.createTime = createTime;
        return castSelf();
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     * @return 子类自身
     */
    public T setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return castSelf();
    }

    /**
     * 将当前实例转换为子类泛型视图。
     *
     * @return 当前实例
     */
    @SuppressWarnings("unchecked")
    protected final T castSelf() {
        return (T) this;
    }
}
