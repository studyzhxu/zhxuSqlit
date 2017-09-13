package com.zhxu.sqlit.library.db;

import java.util.List;

/**
 * <p>Description: 接口定义增删改查
 *
 * @author xzhang
 */

public interface IBaseDao<T> {

    /**
     * 插入一个对象到数据库
     * @param entity 要插入的对象
     * @return
     */
    public Long insert(T entity) ;

    /**
     * 更新
     * @param entity
     * @param where
     * @return
     */
    public int update(T entity, T where) ;

    /**
     * 删除
     * @param where
     * @return
     */
    public int delete(T where);

    /**
     * 查询
     * @param where
     * @return
     */
    public List<T> query(T where) ;

    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) ;
}
