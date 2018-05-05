package client;

import data.ElasticSearchRecord;
import mapper.ElasticSearchRecordMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import util.MessageUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ElasticSearchClient<T> {
    private final static int port = 9300;
    private final static String host = "101.132.158.220";
    private static Logger logger = Logger.getLogger(ElasticSearchClient.class);
    private TransportClient client;
    private static BlockingQueue<List<ElasticSearchRecord>> blockingQueue = new LinkedBlockingQueue<List<ElasticSearchRecord>>(10);
    private static int mysqlStart = 356000;
    private static int elasticStart = 356000;
    private static int step = 1000;


    public void connect() {
        try {
            client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
            //logger.info(client.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        client.close();
    }

    public void add(ElasticSearchRecord record) {
        IndexResponse response = null;
        try {
            response = client.prepareIndex("userrecords", "record").setSource(XContentFactory.jsonBuilder()
                    .startObject().field("fromUserId", record.getFromUserId())
                    .field("ownerId", record.getOwnerId())
                    .field("receivedMessage", record.getReceivedMessage())
                    .field("sentMessage", record.getSentMessage())
                    .field("messageTime", record.getMessageTime())
                    .endObject()).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //logger.info(response);
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            public void run() {
                String resource = "mybatis-config.xml";
                SqlSession session = null;
                try {
                    InputStream inputStream = Resources.getResourceAsStream(resource);
                    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
                    session = sqlSessionFactory.openSession();
                    ElasticSearchRecordMapper mapper = session.getMapper(ElasticSearchRecordMapper.class);
                    while (true) {
                        List<ElasticSearchRecord> movieSearchMessages = mapper.getElasticSearchRecord(mysqlStart, step);
                        if (movieSearchMessages.size()==0)
                            break;
                        blockingQueue.offer(movieSearchMessages);
                        logger.info("mysql当前进度" + String.valueOf(mysqlStart));
                        mysqlStart += step;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    session.close();
                    logger.info("结束");
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                ElasticSearchClient client = new ElasticSearchClient();
                client.connect();
                try {
                    while (true) {
                        List<ElasticSearchRecord> movieSearchMessages = blockingQueue.take();
                        for (ElasticSearchRecord m : movieSearchMessages) {
                            String xmlContent = m.getSentMessage();
                            if(xmlContent!=null) {
                                Map<String, String> xmlMapper = MessageUtil.xmlToMap(xmlContent);
                                m.setFromUserId(xmlMapper.get("FromUserName"));
                            }
                            client.add(m);
                        }
                        logger.info("elastic当前进度" + String.valueOf(elasticStart));
                        elasticStart += step;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    client.close();
                }

            }
        }).start();

    }
}
