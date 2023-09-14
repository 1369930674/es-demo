package com.hexin.dao;

import com.hexin.entity.AudioInfo;
import com.hexin.entity.SyncParam;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AudioTextDao {
    AudioInfo selectText(Map<String, Object> param);

    /**
     * 获取所有表名 t_audio_text_2020_08 分表表名
     *
     * @param map
     * @return
     */
    List<String> selectTableNames(Map<String, String> map);

    /**
     * 通过文件id获取 记录创建时间，用于拼接txt表名
     *
     * @param id
     * @return
     */
    Map<String, Object> getAudioCreateTimeById(int id);

    List<Integer> getFileIds(SyncParam syncParam);

    AudioInfo getFileInfo(Integer fileId);

    List<AudioInfo> selectTextBatch(Map<String, Object> param);
}
