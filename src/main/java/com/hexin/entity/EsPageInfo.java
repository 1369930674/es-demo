package com.hexin.entity;

import lombok.Data;

import java.util.List;

/**
 * es搜索返回结果
 * @param <T>
 */
@Data
public class EsPageInfo<T> {

    /**
     * 总条数
     */
    private Long total;
    /**
     * 结果集
     */
    private List<T> list;
}
