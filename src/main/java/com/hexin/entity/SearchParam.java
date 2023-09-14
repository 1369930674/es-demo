package com.hexin.entity;

import lombok.Data;

/**
 * es搜索参数
 */
@Data
public class SearchParam {
    private Integer userId;
    private String query;
    private Integer page;
    private Integer pageSize;
}
