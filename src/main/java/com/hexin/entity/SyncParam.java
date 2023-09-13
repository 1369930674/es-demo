package com.hexin.entity;

import lombok.Data;

/**
 * 同步数据参数
 */
@Data
public class SyncParam {
    private String startTime;
    private String endTime;
    private Integer userId;
}
