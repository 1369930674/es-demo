package com.hexin.service;

import com.hexin.config.Config;
import com.hexin.entity.AudioInfo;
import com.hexin.entity.NGramAnalyzer;
import com.hexin.entity.SearchPageInfo;
import com.hexin.entity.SearchParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class LuceneService {
    @Autowired
    private Config config;
    @Autowired
    private AudioService audioService;

    private IndexWriter indexWriter;
    private IndexReader indexReader;
    private IndexSearcher searcher;

    @PostConstruct
    public void init() throws IOException {
        Directory directory = FSDirectory.open(Paths.get(config.getLuceneIndexDir()));
        Analyzer analyzer = new NGramAnalyzer(1, 1);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        indexReader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(indexReader);
        searcher.setQueryCache(new LRUQueryCache(1000, 100));
    }

    @PreDestroy
    // 在应用程序关闭时关闭 IndexReader
    public void close() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (indexReader != null) {
            indexReader.close();
        }
    }

    /**
     * 批量导入
     *
     * @param audioInfoList
     * @throws IOException
     */
    public void insertAudioInfoBatch(List<AudioInfo> audioInfoList) throws IOException {
        List<Document> documentList = new ArrayList<>();
        for (AudioInfo audioInfo : audioInfoList) {
            Document document = getDocument(audioInfo);
            documentList.add(document);
        }
        indexWriter.addDocuments(documentList);
        indexWriter.commit();

    }

    /**
     * 单个导入
     *
     * @param audioInfo
     * @throws IOException
     */
    public void insertAudioInfo(AudioInfo audioInfo) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(config.getLuceneIndexDir()));
        Analyzer analyzer = new NGramAnalyzer(1, 1);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        try (IndexWriter indexWriter = new
                IndexWriter(directory, indexWriterConfig)) {
            Document document = getDocument(audioInfo);
            indexWriter.addDocument(document);
            indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询
     *
     * @param param
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public SearchPageInfo<AudioInfo> searchDocument(SearchParam param) throws IOException, ParseException {
        long startTime = System.currentTimeMillis();
        Query query = buildQuery(param);
        Directory directory = FSDirectory.open(Paths.get(config.getLuceneIndexDir()));
        SearchPageInfo<AudioInfo> searchPageInfo = new SearchPageInfo<>();
        TopDocs topDocs = searcher.search(query, 10000);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        long searchEndTime = System.currentTimeMillis();

        //排序
        sort(scoreDocs);
        //分页处理
        ScoreDoc[] pageScoreDocs = fetchPage(scoreDocs, param.getPage(), param.getPageSize());
        //获取audioinfo并进行高亮等处理
        List<AudioInfo> audioInfoList = getAudioInfosFromDocs(query, searcher, pageScoreDocs);
        searchPageInfo.setList(audioInfoList);
        //设置总数
        long total = topDocs.totalHits.value;
        searchPageInfo.setTotal(total);

        long endTime = System.currentTimeMillis();
        log.info("search time: {}", (searchEndTime - startTime));
        log.info("total time: {} total: {}", endTime - startTime, total);
        return searchPageInfo;
    }

    /**
     * 分页处理
     *
     * @param scoreDocs
     * @param page
     * @param pageSize
     * @return
     */
    private ScoreDoc[] fetchPage(ScoreDoc[] scoreDocs, Integer page, Integer pageSize) {
        if (page == null || pageSize == null) {
            return scoreDocs;
        }
        int totalResults = scoreDocs.length;
        // 起始索引
        int start = (page - 1) * pageSize;
        // 结束索引，不超过结果总数
        int end = Math.min(start + pageSize, totalResults);
        if (start >= end) {
            // 返回空数组表示无结果
            return new ScoreDoc[0];
        }
        return Arrays.copyOfRange(scoreDocs, start, end);
    }


    /**
     * 记过排序，按照匹配度降序
     *
     * @param scoreDocs
     */
    private void sort(ScoreDoc[] scoreDocs) {
        Arrays.sort(scoreDocs, new Comparator<ScoreDoc>() {
            @Override
            public int compare(ScoreDoc doc1, ScoreDoc doc2) {
                // 匹配度降序排序
                if (doc1.score < doc2.score) {
                    return 1;
                } else if (doc1.score > doc2.score) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * 从查询结果中获取audioInfo对象
     *
     * @param query
     * @param searcher
     * @param scoreDocs
     * @return
     * @throws IOException
     * @throws InvalidTokenOffsetsException
     */
    private List<AudioInfo> getAudioInfosFromDocs(Query query, IndexSearcher searcher, ScoreDoc[] scoreDocs) {
        List<AudioInfo> audioInfoList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个具有固定线程数量的线程池
        CountDownLatch latch = new CountDownLatch(scoreDocs.length); // 创建一个计数器，用于等待所有子线程完成
        for (ScoreDoc scoreDoc : scoreDocs) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Document doc = searcher.doc(scoreDoc.doc);
                        if (doc.get("fileId") != null) {
                            AudioInfo audioInfo = audioService.selectAudioInfoByFileId(Integer.valueOf(doc.get("fileId")));
                            //高亮
                            highlighter(audioInfo, query);
                            audioInfoList.add(audioInfo);
                        }
                    } catch (Exception e) {
                        log.error("getAudioInfosFromDocs error: {}", e);
                    } finally {
                        latch.countDown(); // 子线程完成任务后减少计数器
                    }

                }
            });
        }
        try {
            latch.await(); // 等待所有子线程完成
        } catch (InterruptedException e) {
            log.error("getAudioInfosFromDocs interrupted: {}", e);
            Thread.currentThread().interrupt();
        }
        executorService.shutdown(); // 关闭线程池
        return audioInfoList;
    }

    /**
     * 添加高亮
     *
     * @param audioInfo
     * @param query
     */
    private void highlighter(AudioInfo audioInfo, Query query) {
        //文稿
        if (StringUtils.isNotBlank(audioInfo.getText())) {
            String highlightText = doHighlight("text", audioInfo.getText(), query, new NGramAnalyzer(1, 1),
                    true, 30);
            if (StringUtils.isNotBlank(highlightText)) {
                audioInfo.setText(highlightText);
            }
        }
        //文件名称
        if (StringUtils.isNotBlank(audioInfo.getFileName())) {
            String highlightFileName = doHighlight("fileName", audioInfo.getFileName(), query, new NGramAnalyzer(1, 1),
                    false, null);
            if (StringUtils.isNotBlank(highlightFileName)) {
                audioInfo.setFileName(highlightFileName);
            }
        }
    }

    /**
     * 获取高亮文稿
     *
     * @param field
     * @param content
     * @param query
     * @param analyzer
     * @param isLimitContextSize
     * @param limitContextSize
     * @return
     */
    public String doHighlight(String field, String content, Query query, Analyzer analyzer, boolean isLimitContextSize,
                              Integer limitContextSize) {
        try {
            QueryScorer scorer = new QueryScorer(query);
            // 创建高亮器
            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), scorer);
            if (isLimitContextSize) {
                //设置显示高亮前后多少字符
                highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, limitContextSize));
            }
            // 获取文本的 TokenStream
            TokenStream tokenStream = TokenSources.getTokenStream(field, content, analyzer);
            // 执行高亮操作
            String highlightedText = highlighter.getBestFragment(tokenStream, content);
            return highlightedText;
        } catch (Exception e) {
            log.error("get highlight text error: {}", e);
            return null;
        }
    }

    /**
     * audioInfo转成document
     *
     * @param audioInfo
     * @return
     */
    private Document getDocument(AudioInfo audioInfo) {
        Document document = new Document();
        if (audioInfo.getFileId() != null) {
            document.add(new StringField("fileId", String.valueOf(audioInfo.getFileId()), Field.Store.YES));
        }
        if (audioInfo.getUserId() != null) {
            document.add(new StringField("userId", String.valueOf(audioInfo.getUserId()), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(audioInfo.getText())) {
            document.add(new TextField("text", audioInfo.getText(), Field.Store.NO));
        }
        if (StringUtils.isNotBlank(audioInfo.getFileName())) {
            document.add(new TextField("fileName", audioInfo.getFileName(), Field.Store.NO));
        }
        if (StringUtils.isNotBlank(audioInfo.getAbstracts())) {
            document.add(new TextField("abstracts", audioInfo.getAbstracts(), Field.Store.NO));
        }
        if (StringUtils.isNotBlank(audioInfo.getKeywords())) {
            document.add(new TextField("keywords", audioInfo.getKeywords(), Field.Store.NO));
        }
        if (StringUtils.isNotBlank(audioInfo.getCreateTime())) {
            document.add(new StoredField("createTime", audioInfo.getCreateTime()));
        }
        return document;
    }

    /**
     * 构建查询query
     *
     * @param param
     * @return
     * @throws ParseException
     */
    private Query buildQuery(SearchParam param) throws ParseException {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        if (param.getUserId() != null) {
            // 完全匹配 userId
            TermQuery userIdQuery = new TermQuery(new Term("userId", String.valueOf(param.getUserId())));
            queryBuilder.add(userIdQuery, BooleanClause.Occur.MUST);
        }
        if (StringUtils.isNotBlank(param.getQuery())) {
            // 匹配 fileName 或 text
            QueryParser queryParser = new QueryParser("text", new NGramAnalyzer(1, 1));
            Query textQuery = queryParser.parse(param.getQuery());
            Query fileNameQuery = new TermQuery(new Term("fileName", param.getQuery()));
            queryBuilder.add(new BooleanClause(textQuery, BooleanClause.Occur.SHOULD));
            queryBuilder.add(new BooleanClause(fileNameQuery, BooleanClause.Occur.SHOULD));
            queryBuilder.setMinimumNumberShouldMatch(1);
        }

        return queryBuilder.build();
    }

    public Integer getIndexTotal() throws IOException {
        Directory directory = FSDirectory.open(Paths.get(config.getLuceneIndexDir()));
        IndexReader reader = DirectoryReader.open(directory);  // indexDirectory 表示索引存储的位置
        int total = reader.numDocs();
        log.info("index total: {}", total);
        return total;
    }
}
