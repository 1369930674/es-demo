package com.hexin.controller;

import com.hexin.entity.EsAudioInfo;
import com.hexin.entity.EsPageInfo;
import com.hexin.entity.EsSearchParam;
import com.hexin.entity.SyncParam;
import com.hexin.service.AudioService;
import com.hexin.service.EsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/es")
@Slf4j
public class EsController {
    @Autowired
    private AudioService audioService;
    @Autowired
    private EsService esService;

    /**
     * 同步sql数据至es
     *
     * @return
     */
    @RequestMapping("/start")
    public String syncAudioInfoFromSqlToEs(SyncParam syncParam) {
        try {
            audioService.syncAudioInfoFromSqlToEs(syncParam);
            return "success";
        } catch (Exception e) {
            log.info("sync audio info from sql to es error: {}", e);
            return "success";
        }
    }

    @RequestMapping("/search")
    public EsPageInfo<EsAudioInfo> search(Integer userId, String query) {
        EsSearchParam esSearchParam = new EsSearchParam();
        esSearchParam.setPage(1);
        esSearchParam.setPageSize(10);
        esSearchParam.setUserId(userId);
        esSearchParam.setQuery(query);
        EsPageInfo<EsAudioInfo> esAudioInfoEsPageInfo = esService.multiConditionSearch(esSearchParam);
        return esAudioInfoEsPageInfo;
    }

}
