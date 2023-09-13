import com.hexin.EsStudyApplication;
import com.hexin.dao.AudioTextDao;
import com.hexin.entity.*;
import com.hexin.mq.SendService;
import com.hexin.service.AudioService;
import com.hexin.service.EsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsStudyApplication.class)
@Slf4j
public class Test {

    @Autowired
    private EsService esService;
    @Autowired
    AudioTextDao audioTextDao;
    @Autowired
    private AudioService audioService;
    @Autowired
    private SendService sendService;

    @org.junit.Test
    public void testInsert() {
//        Map<String, Object> param = new HashMap<>();
//        param.put("tableName", "t_audio_text_2023_08");
//        param.put("fileId", 1350625);
//        AudioInfo audioInfo = audioTextDao.selectText(param);
//        System.out.println(1);
        SyncParam syncParam = new SyncParam();
//        syncParam.setUserId(530756924);
//        audioService.syncAudioInfoFromSqlToEs(109644);
        audioService.syncAudioInfoFromSqlToEs(109645);
//
//        audioService.syncAudioInfoFromSqlToEs(50761);
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
        EsSearchParam esSearchParam = new EsSearchParam();
        esSearchParam.setPage(1);
        esSearchParam.setPageSize(20);
        esSearchParam.setUserId(530756924);
        esSearchParam.setQuery("");
        EsPageInfo<EsAudioInfo> esAudioInfoEsPageInfo = esService.multiConditionSearch(esSearchParam);
        System.out.println(1);
    }


}
