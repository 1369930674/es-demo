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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class AudioService {
    @Autowired
    private AudioTextDao audioTextDao;
    @Autowired
    private LuceneService luceneService;


    /**
     * 根据fileId查询音频文件信息
     *
     * @param fileId
     * @return
     */
    public AudioInfo selectAudioInfoByFileId(Integer fileId) {
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

    /**
     * 批量导入数据到lucene
     * @param syncParam
     */

    public void syncAudioInfoFromSqlToLucene(SyncParam syncParam) {
        List<Integer> fileIds = audioTextDao.getFileIds(syncParam);
        log.info("sync file from sql to es, sysncParam: {}, file size: {}", JsonUtils.toJSONString(syncParam),
                fileIds.size());
        Integer size = 1000;
        List<List<Integer>> lists = IntStream.range(0, (fileIds.size() + size - 1) / size)
                .mapToObj(i -> fileIds.subList(i * size, Math.min((i + 1) * size, fileIds.size())))
                .collect(Collectors.toList());
        for (List<Integer> ids : lists) {
            List<AudioInfo> audioInfos = selectAudioInfoFromLuceneBatch(ids);
            try {
                luceneService.insertAudioInfoBatch(audioInfos);
                log.info("insert success");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        //多线程会导致枪锁失败
        /*ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个具有固定线程数量的线程池
        for (List<Integer> ids : lists) {
            List<AudioInfo> audioInfos = ids.stream().map(id -> selectAudioInfoByFileId(id)).collect(Collectors.toList());
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        try {
                            luceneService.insertAudioInfoBatch(audioInfos);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }*/
//        executorService.shutdown(); // 关闭线程池

    }

    /**
     * 从lucene中批量查询fileIds
     * @param fileIds
     * @return
     */
    private List<AudioInfo> selectAudioInfoFromLuceneBatch(List<Integer> fileIds) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "t_audio_text_2020_07");
        param.put("fileIds", fileIds);
        List<AudioInfo> audioInfoList = audioTextDao.selectTextBatch(param);
        audioInfoList.stream().forEach(audioInfo -> {
            if (audioInfo.getAsrResult() != null) {
                String asrResult = GzipUtils.decompressGzipText(audioInfo.getAsrResult());
                audioInfo.setText(getTextWithoutSpk(asrResult));
            }
        });

        return audioInfoList;
    }

    /**
     * 根据fileId导入luncene
     * @param fileId
     */
    public void syncAudioInfoFromSqlToLucene(Integer fileId) {
        AudioInfo audioInfo = selectAudioInfoByFileId(fileId);
        try {
            luceneService.insertAudioInfo(audioInfo);
            log.info("fileId : {}, success", fileId);
        } catch (IOException e) {
            log.error("fileId: {} inser text into es error: {}", fileId, e);
        }
    }
}
