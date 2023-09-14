package com.hexin.controller;

import com.hexin.entity.*;
import com.hexin.service.AudioService;
import com.hexin.service.LuceneService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/es")
@Slf4j
public class EsController {
    @Autowired
    private AudioService audioService;
    @Autowired
    private LuceneService luceneService;

    /**
     * 同步sql数据至es
     *
     * @return
     */
    @RequestMapping("/start")
    public String syncAudioInfoFromSqlToEs(SyncParam syncParam) {
        try {
            audioService.syncAudioInfoFromSqlToLucene(syncParam);
            return "success";
        } catch (Exception e) {
            log.info("sync audio info from sql to es error: {}", e);
            return "success";
        }
    }

    @RequestMapping("/search")
    public List<AudioInfo> search(Integer userId, String query) {
        SearchParam searchParam = new SearchParam();
        searchParam.setPage(1);
        searchParam.setPageSize(10);
        searchParam.setUserId(userId);
        searchParam.setQuery(query);
        List<AudioInfo> esAudioInfoEsPageInfo = null;
        try {
            esAudioInfoEsPageInfo = luceneService.searchDocument(searchParam);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return esAudioInfoEsPageInfo;
    }

}
