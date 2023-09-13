package com.hexin.entity;

import lombok.Data;

/**
 * es中音频信息
 */
@Data
public class EsAudioInfo {
    private Integer fileId;
    private Integer userId;
    private String fileName;
    private String text;
    private String abstracts;
    private String keywords;
    private String createTime;
}
