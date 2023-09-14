import com.hexin.EsStudyApplication;
import com.hexin.dao.AudioTextDao;
import com.hexin.entity.*;
import com.hexin.mq.SendService;
import com.hexin.service.AudioService;
import com.hexin.service.LuceneService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

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
        searchParam.setPage(2);
        searchParam.setPageSize(20);
        searchParam.setUserId(525498756);
        searchParam.setQuery("增强内在力量3");
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

}
