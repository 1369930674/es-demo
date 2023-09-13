package com.hexin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AudioInfo {
    private Integer fileId;
    private Integer userId;
    private String fileName;
    private String text;
    private String keywords;
    private String asrResult;
    private String abstracts;
    private String createTime;
}
