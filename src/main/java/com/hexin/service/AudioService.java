package com.hexin.service;

import com.hexin.dao.AudioTextDao;
import com.hexin.entity.AudioInfo;
import com.hexin.entity.Constant;
import com.hexin.entity.SyncParam;
import com.hexin.util.GzipUtils;
import com.hexin.util.json.JSONArray;
import com.hexin.util.json.JSONObject;
import com.hexin.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class AudioService {
    @Autowired
    private AudioTextDao audioTextDao;
    @Autowired
    private EsService esService;

    /**
     * 同步时间范围内的文件到es
     *
     * @param syncParam
     */
    public void syncAudioInfoFromSqlToEs(SyncParam syncParam) {
        List<Integer> fileIds = audioTextDao.getFileIds(syncParam);
        log.info("sync file from sql to es, sysncParam: {}, file size: {}", JsonUtils.toJSONString(syncParam),
                fileIds.size());
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个具有固定线程数量的线程池
        for (Integer fileId : fileIds) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (this){
                        syncAudioInfoFromSqlToEs(fileId);
                    }
                }
            });
        }
        executorService.shutdown(); // 关闭线程池
    }


    /**
     * sql数据同步es
     *
     * @param fileId
     */
    public void syncAudioInfoFromSqlToEs(Integer fileId) {
        AudioInfo audioInfo = selectAudioInfoByFileId(fileId);
        try {
            esService.insertAudioInfo(audioInfo);
        } catch (IOException e) {
            log.error("fileId: {} inser text into es error: {}", fileId, e);
        }
    }

    /**
     * 删除es音频文件
     * @param fileId
     */
    public void deleteAudioInfo(Integer fileId){
        try {
            esService.deleteAudioInfo(fileId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据fileId查询音频文件信息
     * @param fileId
     * @return
     */
    private AudioInfo selectAudioInfoByFileId(Integer fileId) {
        Map<String, Object> param = new HashMap<>();
        String tableName = getTableNameByFileId(fileId);
        param.put("tableName", tableName);
        param.put("fileId", fileId);
        AudioInfo audioInfo = audioTextDao.selectText(param);
        if (audioInfo != null && audioInfo.getAsrResult() != null) {
            String asrResult = GzipUtils.decompressGzipText(audioInfo.getAsrResult());
            String textWithoutSpk = getTextWithoutSpk(asrResult);
            audioInfo.setText(textWithoutSpk);
        } else {
            audioInfo = audioTextDao.getFileInfo(fileId);
        }
        return audioInfo;
    }

    /**
     * 根据fileId获取t_audio_text_表名称
     *
     * @param fileId
     * @return
     */
    public String getTableNameByFileId(int fileId) {
        String tableName;
        Map map = audioTextDao.getAudioCreateTimeById(fileId);
        if (null == map) {
//            throw new BaseBusinessException(ResultEnum.RECORD_NOT_FIND);
        }
        String createTime = map.get("createTime").toString();
        tableName = Constant.T_AUDIO_TEXT + createTime;
        return tableName;
    }


    /**
     * 获取不带说话人的文本
     *
     * @param result
     * @return
     */
    public String getTextWithoutSpk(String result) {
        JSONArray resultJsonArray = JSONArray.parseArray(result);
        StringBuilder ans = new StringBuilder();
        for (int i = 0; i < resultJsonArray.size(); i++) {
            JSONObject data = resultJsonArray.getJSONObject(i);
            String dataString = data.getString("data");
            JSONArray dataJsonArray = JSONArray.parseArray(dataString);
            StringBuilder tempText = new StringBuilder();
            for (int j = 0; j < dataJsonArray.size(); j++) {
                JSONObject jsonObject = dataJsonArray.getJSONObject(j);
                String text = jsonObject.getString("text");
                if (StringUtils.isNotBlank(text)) {
                    tempText.append(text);
                }
            }
            if (StringUtils.isNotBlank(tempText.toString())) {
                ans.append(tempText.toString());
            }
        }
        return ans.toString();
    }
}
