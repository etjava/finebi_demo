package com.et;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/finebi")
public class FineBIServlet extends HttpServlet {

    // FineBI 服务器配置
    private static final String FINEBI_SERVER = "http://localhost:37799";
    private static final String FINEBI_USERNAME = "root";
    private static final String FINEBI_PASSWORD = "root";

    // 存储每个用户的 FineBI 会话上下文
    private final Map<String, String> userContexts = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        String sessionId = req.getSession().getId();
        HttpSession session = req.getSession();
        switch (action) {
            case "login":
                handleLogin(sessionId, req, resp,session);
                break;
            case "chartList":
                handleChartList(sessionId, req, resp,session);
                break;
            case "getToken":
                getToken(sessionId, req, resp,session);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }

    /*返回token信息*/
    private void getToken(String sessionId, HttpServletRequest req, HttpServletResponse resp, HttpSession session) throws IOException {
        JSONObject res = new JSONObject();
        String accessToken = getDemoToken();// demo token
        res.put("status","success");
        res.put("token",session.getAttribute(sessionId));
        resp.setHeader("Content-Type", "application/json; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(res);
        out.flush();
    }

    /*加载图表列表*/
    private void handleChartList(String sessionId, HttpServletRequest req, HttpServletResponse resp,HttpSession session) throws IOException {
        try {
            // 创建 HttpClient 和上下文
            BasicCookieStore cookieStore = new BasicCookieStore();
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieStore);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build();

            CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setDefaultCookieStore(cookieStore)
                    .build();

            try {

                // 图表列表请求
                String chartListUrl = FINEBI_SERVER + "/webroot/decision/v5/api/dashboard/user/info?op=api&cmd=get_all_reports_data&fine_auth_token="+session.getAttribute(sessionId);
//                String chartListUrl2 = FINEBI_SERVER + "/webroot/decision/v5/api/dashboard/user/info?op=api&cmd=get_all_reports_data&fine_auth_token=" +session.getAttribute(sessionId)+
//                        "&_=1748705124404";

                URIBuilder uriBuilder = new URIBuilder(chartListUrl);
                uriBuilder.addParameter("fine_auth_token", session.getAttribute(sessionId).toString());
                uriBuilder.addParameter("_", String.valueOf(System.currentTimeMillis()));
                HttpGet httpGet = new HttpGet(chartListUrl);

                // 设置必要的请求头
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");

                // 执行登录请求
                HttpResponse loginResponse = client.execute(httpGet, context);
                int statusCode = loginResponse.getStatusLine().getStatusCode();
                // 检查登录是否成功
                if (statusCode == 302 || statusCode == 200) {
                    // 解析JSONP响应
                    String responseBody = EntityUtils.toString(loginResponse.getEntity());
                    String jsonStr = extractJsonFromJsonp(responseBody);
                    JSONObject jsonObject = new JSONObject(jsonStr);
                    String data = jsonObject.get("data").toString();
                    JSONObject dataObj = new JSONObject(data);
                    JSONArray dashboards = new JSONArray(dataObj.get("dashboards").toString());
                    JSONObject da = new JSONObject(dashboards.get(0).toString());
                    // 返回列表信息
                    JSONObject res = new JSONObject();
                    res.put("status","success");
                    res.put("data",dashboards);
                    resp.setHeader("Content-Type", "application/json; charset=UTF-8");

                    PrintWriter out = resp.getWriter();
                    out.print(res);
                    out.flush();
                } else {
                    HttpEntity entity = loginResponse.getEntity();
                    String responseBody = entity != null ? EntityUtils.toString(entity) : "";
                    // 返回失败响应
                    resp.setContentType("application/json");
                    PrintWriter out = resp.getWriter();
                    out.print("{\"status\": \"failed\", \"message\": \"图表加载失败: " + statusCode + "\", \"details\": \"" +
                            responseBody.replaceAll("\"", "\\\\\"") + "\"}");
                    out.flush();
                    throw new IOException("Login failed: " + statusCode + ", Response: " + responseBody);
                }
            } finally {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "图表加载失败: " + e.getMessage());
        }
    }

    // 处理登录请求，获取 FineBI 会话
    private void handleLogin(String sessionId, HttpServletRequest req, HttpServletResponse resp, HttpSession session) throws IOException {
        try {
            // 创建 HttpClient 和上下文
            BasicCookieStore cookieStore = new BasicCookieStore();
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieStore);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build();

            CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setDefaultCookieStore(cookieStore)
                    .build();

            try {
                // 登录请求
                String loginUrl = FINEBI_SERVER + "/webroot/decision/login/cross/domain";
                URIBuilder uriBuilder = new URIBuilder(loginUrl);
                uriBuilder.addParameter("fine_username", FINEBI_USERNAME);
                uriBuilder.addParameter("fine_password", FINEBI_PASSWORD);
                uriBuilder.addParameter("validity", "-1");
                HttpGet httpGet = new HttpGet(uriBuilder.build());
                // 设置必要的请求头
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
                // 执行登录请求
                HttpResponse loginResponse = client.execute(httpGet, context);
                String responseBody22 = EntityUtils.toString(loginResponse.getEntity());
                // 解析JSONP响应
                String jsonStr = extractJsonFromJsonp(responseBody22);
                JSONObject jsonObject = new JSONObject(jsonStr);
                int statusCode = loginResponse.getStatusLine().getStatusCode();

                // 检查登录是否成功
                if (statusCode == 302 || statusCode == 200) {
                    // 保存会话上下文

                    userContexts.put(sessionId, jsonObject.get("accessToken").toString());
                    session.setAttribute(sessionId, jsonObject.get("accessToken").toString());
                    // 返回成功响应
                    resp.setContentType("application/json");
                    PrintWriter out = resp.getWriter();
                    out.print("{\"status\": \"success\", \"message\": \"登录成功\"}");
                    out.flush();

                    addCookie("fine_auth_token",jsonObject.get("accessToken").toString(),resp);
                } else {
                    HttpEntity entity = loginResponse.getEntity();
                    String responseBody = entity != null ? EntityUtils.toString(entity) : "";
                    // 返回失败响应
                    resp.setContentType("application/json");
                    PrintWriter out = resp.getWriter();
                    out.print("{\"status\": \"failed\", \"message\": \"登录失败: " + statusCode + "\", \"details\": \"" +
                            responseBody.replaceAll("\"", "\\\\\"") + "\"}");
                    out.flush();
                    throw new IOException("Login failed: " + statusCode + ", Response: " + responseBody);
                }
            } finally {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed: " + e.getMessage());
        }
    }

    private void addCookie(String fine_auth_token, String accessToken,HttpServletResponse response) {
        Cookie cookie = new Cookie(fine_auth_token,accessToken);
        cookie.setMaxAge(1800); // 单位：秒
        response.addCookie(cookie);
    }

    /*临时替换为demo用户token*/
    private String getDemoToken() throws IOException {
        String res = "";
        HttpClient httpClient = HttpClients.createDefault();
        String url = "http://localhost:37799/webroot/decision/login/cross/domain?fine_username=demo&fine_password=demo&validity=-1";
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

                String jsonStr = extractJsonFromJsonp(responseBody);
                JSONObject jsonObject = new JSONObject(jsonStr);
                res = jsonObject.get("accessToken").toString();
            }
        } finally {

        }
        return res;
    }

    // 从JSONP响应中提取JSON内容
    private static String extractJsonFromJsonp(String jsonp) {
        // 假设JSONP格式为 callbackFunction({"key":"value"})
        int startIndex = jsonp.indexOf('(');
        int endIndex = jsonp.lastIndexOf(')');

        if (startIndex > 0 && endIndex > startIndex) {
            return jsonp.substring(startIndex + 1, endIndex);
        }

        // 如果格式不匹配，直接返回原始内容
        return jsonp;
    }
}