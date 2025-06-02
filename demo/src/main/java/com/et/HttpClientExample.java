package com.et;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpClientExample {
    public static void main(String[] args) {
        HttpClient httpClient = HttpClients.createDefault();

        try {
            // 1. 执行GET请求示例
            executeGetRequest(httpClient);

            // 2. 执行POST请求示例
            executePostRequest(httpClient);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 对于CloseableHttpClient需要关闭，本示例使用的是普通HttpClient无需显式关闭
        }
    }

    /**
     * 执行GET请求并处理响应
     */
    private static void executeGetRequest(HttpClient httpClient) throws IOException {
        String url = "http://localhost:37799/webroot/decision/login/cross/domain?fine_username=root&fine_password=root&validity=-1";
        HttpGet httpGet = new HttpGet(url);

        // 可选：设置请求头
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("Authorization", "Bearer your_access_token");

        System.out.println("执行GET请求: " + url);
        HttpResponse response = httpClient.execute(httpGet);

        try {
            // 获取响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("状态码: " + statusCode);

            // 获取响应实体
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // 读取响应内容
                String responseBody = EntityUtils.toString(entity);
                System.out.println("响应内容: " + responseBody);

                // 示例：解析JSON响应
                if (responseBody.startsWith("{") || responseBody.startsWith("[")) {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    System.out.println("JSON解析示例: " + jsonObject.toString(2));
                }
            }
        } finally {
            // 确保释放响应资源
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    /**
     * 执行POST请求并处理响应
     */
    private static void executePostRequest(HttpClient httpClient) throws IOException {
        String url = "http://localhost:37799/webroot/decision/login/cross/domain?fine_username=root&fine_password=root&validity=-1";
        HttpPost httpPost = new HttpPost(url);

        // 设置请求头
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Authorization", "Bearer your_access_token");

        // 设置POST参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("key1", "value1"));
        params.add(new BasicNameValuePair("key2", "value2"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        System.out.println("\n执行POST请求: " + url);
        HttpResponse response = httpClient.execute(httpPost);

        try {
            // 获取响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("状态码: " + statusCode);

            // 获取响应实体
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // 读取响应内容
                String responseBody = EntityUtils.toString(entity);
                System.out.println("响应内容: " + responseBody);
            }
        } finally {
            // 确保释放响应资源
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }
}    