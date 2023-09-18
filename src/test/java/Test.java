import com.hexin.EsStudyApplication;
import com.hexin.dao.AudioTextDao;
import com.hexin.entity.*;
import com.hexin.mq.SendService;
import com.hexin.service.AudioService;
import com.hexin.service.LuceneService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsStudyApplication.class)
@Slf4j
public class Test {

    @Autowired
    AudioTextDao audioTextDao;
    @Autowired
    private AudioService audioService;
    @Autowired
    private SendService sendService;
    @Autowired
    private LuceneService luceneService;

    @org.junit.Test
    public void testInsert() throws IOException {
        SyncParam syncParam = new SyncParam();
        syncParam.setUserId(525498756);
        audioService.syncAudioInfoFromSqlToLucene(syncParam);
        System.out.println(1);
    }

    @org.junit.Test
    public void testMqSend() {
        EsMessage esMessage = new EsMessage();
        esMessage.setInfoType(Constant.MQ_INSERT);
        sendService.sendMessage(esMessage, Constant.QUEUE_NAME);
    }


    @org.junit.Test
    public void testQuery() {
        SearchParam searchParam = new SearchParam();
        searchParam.setPage(1);
        searchParam.setPageSize(10);
        searchParam.setUserId(525498756);
        searchParam.setQuery("七天告别失眠");
        try {
            SearchPageInfo<AudioInfo> searchPageInfo = luceneService.searchDocument(searchParam);
            System.out.println(searchPageInfo.getList().size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        System.out.println(1);
    }

    @org.junit.Test
    public void testIndex(){
        String indexPath = "./lucene";

        try {
            // 打开索引目录
            FSDirectory directory = FSDirectory.open(Paths.get(indexPath));

            // 创建IndexReader对象
            IndexReader reader = DirectoryReader.open(directory);

            // 获取索引段信息
            List<LeafReaderContext> leafContexts = reader.leaves();
            for (LeafReaderContext leafContext : leafContexts) {
                System.out.println("Document Count: " + leafContext.reader().numDocs());
                System.out.println("Field Count: " + leafContext.reader().getFieldInfos().size());
                System.out.println();
            }

            // 关闭IndexReader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
