package com.ppwx.easysearch.cf.util;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import javax.sql.DataSource;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className DataSourceFactory
 * @description ���ݿ�����
 * @date 2025/2/11 9:43
 **/
public class DataSourceFactory {

    public static DataSource newDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://127.0.0.1:3306/yanxuan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        return dataSource;
    }

    public static RestHighLevelClient newESClient() {
        RestClientBuilder builder = RestClient.builder(
                new HttpHost("es-nlb-es-zrqe8x61jd.jvessel-open-sh.jdcloud.com", 9200, "http"));
        // �����û���������
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "Dev_ES_JD_1024KB"));
        // ������ʱ����
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(1000);
            requestConfigBuilder.setSocketTimeout(30000);
            requestConfigBuilder.setConnectionRequestTimeout(500);
            return requestConfigBuilder;
        });
        // ����������
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(100);
            httpClientBuilder.setMaxConnPerRoute(100);
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return httpClientBuilder;
        });
        return new RestHighLevelClient(builder);
    }

}
