package com.hexin.entity;

import lombok.Data;

import java.util.List;

/**
 * 搜索返回结果
 * @param <T>
 */
@Data
public class SearchPageInfo<T> {

    /**
     * 总条数
     */
    private Long total;
    /**
     * 结果集
     */
    private List<T> list;
}
