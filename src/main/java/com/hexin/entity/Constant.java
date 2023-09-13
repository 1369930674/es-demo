package com.hexin.entity;

public class Constant {
    /**
     * es中的索引名称
     */
    public static final String INDEX_NAME = "voiceclub";
    /**
     * 转写结果表名前缀
     */
    public static final String T_AUDIO_TEXT = "t_audio_text_";

    /**
     * mq队列名称
     */
    public static final String QUEUE_NAME = "voice_bank_es";

    //mq 请求类型
    public static final String MQ_INSERT = "insert";
    public static final String MQ_DELETE = "delete";
    public static final String MQ_UPDATE = "update";

}
