package com.hexin.service;

import com.hexin.entity.*;
import com.hexin.util.json.JSONObject;
import com.hexin.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class EsService {
    @Autowired
    RestHighLevelClient restHighLevelClient;

    /**
     * 插入es
     *
     * @param audioInfo
     * @throws IOException
     */
    public void insertAudioInfo(AudioInfo audioInfo) throws IOException {
        IndexRequest indexRequest = new IndexRequest(Constant.INDEX_NAME);
        indexRequest.id(String.valueOf(audioInfo.getFileId()));
        EsAudioInfo esAudioInfo = new EsAudioInfo();
        BeanUtils.copyProperties(audioInfo, esAudioInfo);
        indexRequest.source(JsonUtils.toJSONString(esAudioInfo), XContentType.JSON);
        IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);

        if (indexResponse.getResult() == IndexResponse.Result.CREATED) {
            log.info("fileId: {}, create success", audioInfo.getFileId());
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            log.info("fileId: {}, update success", audioInfo.getFileId());
        } else {
            log.info("fileId: {}, faild result: {}", audioInfo.getFileId(), indexResponse.getResult().toString());

        }
    }

    /**
     * 删除
     *
     * @param fileId
     * @throws IOException
     */
    public void deleteAudioInfo(Integer fileId) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(Constant.INDEX_NAME, String.valueOf(fileId));
        DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);

        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            System.out.println("Document not found: " + fileId);
        } else if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
            System.out.println("Document deleted: " + fileId);
        } else {
            System.out.println("Failed to delete document: " + fileId);
        }
    }

    /**
     * 更新
     *
     * @param audioInfo
     * @throws IOException
     */
    public void updateAudioInfo(AudioInfo audioInfo) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest(Constant.INDEX_NAME, String.valueOf(audioInfo.getFileId()));
        EsAudioInfo esAudioInfo = new EsAudioInfo();
        BeanUtils.copyProperties(audioInfo, esAudioInfo);
        updateRequest.doc(JsonUtils.toJSONString(esAudioInfo), XContentType.JSON);
        UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);

        if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            System.out.println("Document updated: " + audioInfo.getFileId());
        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {
            System.out.println("No changes made to document: " + audioInfo.getFileId());
        } else {
            System.out.println("Failed to update document: " + audioInfo.getFileId());
        }
    }

    /**
     * 多条件查询
     *
     * @param esSearchParam
     * @return
     */
    public EsPageInfo<EsAudioInfo> multiConditionSearch(EsSearchParam esSearchParam) {
        //1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(esSearchParam);
        try {
            //2、执行检索请求
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            log.debug("param：{}, took: {}", esSearchParam.toString(), response.getTook());
            //3、分析响应数据，封装成我们需要的格式
            EsPageInfo<EsAudioInfo> esAudioInfoEsPageInfo = buildSearchResult(esSearchParam, response);
            return esAudioInfoEsPageInfo;
        } catch (IOException e) {
            log.error(e.toString());
//            throw new Exception("搜索服务出了点小差，请稍后再试");
            return null;
        }
    }

    /**
     * 构建结果数据
     *
     * @param esSearchParam
     * @param response
     */
    private EsPageInfo<EsAudioInfo> buildSearchResult(EsSearchParam esSearchParam, SearchResponse response) {
        EsPageInfo<EsAudioInfo> esPageInfo = new EsPageInfo<>();

        SearchHits hits = response.getHits();
        List<EsAudioInfo> esAudioInfos = getEsAudioInfoList(response);
        esPageInfo.setList(esAudioInfos);

        //===============分页信息====================//
        //总记录数
        long total = hits.getTotalHits().value;
        esPageInfo.setTotal(total);
        return esPageInfo;
    }

    /**
     * 封装相应数据，封装为我们需要的格式
     *
     * @param response
     * @return
     */
    private List<EsAudioInfo> getEsAudioInfoList(SearchResponse response) {
        List<EsAudioInfo> esAudioInfos = new ArrayList<>();
        SearchHits hits = response.getHits();
        for (SearchHit hit : hits) {
            String fileId = hit.getId();
            EsAudioInfo esAudioInfo = JSONObject.toJavaObject(hit.getSourceAsString(), EsAudioInfo.class);
            esAudioInfo.setFileId(Integer.valueOf(fileId));
            // 获取高亮字段的值
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField fileNameHighlight = highlightFields.get("fileName");
            HighlightField textHighlight = highlightFields.get("text");
            String fileNameHighlighted = getHighlightedFieldValue(fileNameHighlight, esAudioInfo.getFileName());
            String textHighlighted = getHighlightedFieldValue(textHighlight, esAudioInfo.getText());
            esAudioInfo.setFileName(fileNameHighlighted);
            esAudioInfo.setText(textHighlighted);
            esAudioInfos.add(esAudioInfo);
        }
        return esAudioInfos;
    }

    private String getHighlightedFieldValue(HighlightField highlightField, String originalValue) {
        if (highlightField != null) {
            Text[] fragments = highlightField.fragments();
            if (fragments.length > 0) {
                StringBuilder highlightedValue = new StringBuilder();
                for (Text fragment : fragments) {
                    highlightedValue.append(fragment.string());
                }
                return highlightedValue.toString();
            }
        }
        return originalValue;
    }

    /**
     * 准备检索请求
     *
     * @param esSearchParam
     * @return
     */
    private SearchRequest buildSearchRequest(EsSearchParam esSearchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //关键词搜索
        keywordSearch(esSearchParam, boolQueryBuilder);
        //排序
        sort(searchSourceBuilder, boolQueryBuilder);
        //高亮
        highlighter(searchSourceBuilder);
        //分页
        if (Objects.nonNull(esSearchParam.getPage())) {
            searchSourceBuilder.from((esSearchParam.getPage() - 1) * esSearchParam.getPageSize());
            searchSourceBuilder.size(esSearchParam.getPageSize());
        }
        log.debug("构建的DSL语句 {}", searchSourceBuilder.toString());
        return new SearchRequest(new String[]{Constant.INDEX_NAME}, searchSourceBuilder);
    }

    /**
     * 设置高亮
     *
     * @param searchSourceBuilder
     */
    private void highlighter(SearchSourceBuilder searchSourceBuilder) {
        HighlightBuilder hightLightBuilder = new HighlightBuilder();
        hightLightBuilder.preTags("<font color='orange'>");
        hightLightBuilder.postTags("</font>");
        hightLightBuilder.field("text");
        hightLightBuilder.field("fileName");
        //设置高亮片段个数
        hightLightBuilder.numOfFragments(1);
        //设置高亮展示的字数
        hightLightBuilder.fragmentSize(20);
        searchSourceBuilder.highlighter(hightLightBuilder);
    }

    /**
     * 排序
     *
     * @param searchSourceBuilder
     * @param boolQueryBuilder
     */
    private void sort(SearchSourceBuilder searchSourceBuilder, BoolQueryBuilder boolQueryBuilder) {
//        searchSourceBuilder.sort("createTime", SortOrder.DESC);
        searchSourceBuilder.query(boolQueryBuilder);
    }

    /**
     * 关键词搜索
     *
     * @param esSearchParam
     * @param boolQueryBuilder
     */
    private void keywordSearch(EsSearchParam esSearchParam, BoolQueryBuilder boolQueryBuilder) {
        if (esSearchParam.getUserId() != null) {
            boolQueryBuilder.must(QueryBuilders.termQuery("userId", esSearchParam.getUserId()));
        }
        if (StringUtils.isNotBlank(esSearchParam.getQuery())) {
//            boolQueryBuilder.must(QueryBuilders.multiMatchQuery(esSearchParam.getQuery(), "fileName", "text"));
            boolQueryBuilder.should(QueryBuilders.matchPhraseQuery("fileName", esSearchParam.getQuery()));
            boolQueryBuilder.should(QueryBuilders.matchPhraseQuery("text", esSearchParam.getQuery()));
            boolQueryBuilder.minimumShouldMatch(1);
        }
    }

}
