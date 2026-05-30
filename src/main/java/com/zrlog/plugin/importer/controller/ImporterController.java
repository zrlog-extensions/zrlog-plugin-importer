package com.zrlog.plugin.importer.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.HttpClientUtils;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.PathKit;
import com.zrlog.plugin.common.model.CreateArticleRequest;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.importer.ZipUtil;
import com.zrlog.plugin.importer.convertor.HexoConverter;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImporterController {

    private static final Logger LOGGER = LoggerUtil.getLogger(ImporterController.class);

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final Gson gson = new Gson();

    public ImporterController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void index() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", isDarkMode() ? "dark" : "light");
        data.put("data", gson.toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());

    }

    public void json() {
        response(pageData());
    }

    public void doImport() throws IOException {
        String source = stringValue(params().get("source"));
        if (source.trim().isEmpty()) {
            response(errorMap("请先上传 zip 文件"));
            return;
        }
        File tmpPath = new File(PathKit.getTmpPath() + "/" + UUID.randomUUID() + "/");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Cookie", requestInfo.getHeader().get("Cookie"));
        byte[] bytes = HttpClientUtils.sendGetRequest(source, byte[].class, requestHeaders, session, Duration.ofSeconds(360));
        File zipFile = new File(tmpPath + "/importer.zip");
        IOUtil.writeBytesToFile(bytes, zipFile);
        ZipUtil.unZip(zipFile.toString(), tmpPath + "/");
        File[] files = new File(tmpPath.toString()).listFiles();
        List<CreateArticleRequest> createArticleRequestList = new ArrayList<>();
        for (File file : files) {
            CreateArticleRequest request = null;
            if (file.toString().endsWith(".md") || file.toString().endsWith(".markdown")) {
                try {
                    request = new HexoConverter().parse(file);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "file " + file.getName(), e);
                }
            }
            if (request != null) {
                request.setUserId(requestInfo.getUserId());
                if (Objects.nonNull(request.getType()) && request.getType().isEmpty()) {
                    request.setType("未分类");
                }
                createArticleRequestList.add(request);
            }
        }
        createArticleRequestList.sort(Comparator.comparingLong(o -> o.getReleaseDate().getTime()));
        for (CreateArticleRequest request : createArticleRequestList) {
            session.getResponseSync(ContentType.JSON, request, ActionType.CREATE_ARTICLE, HashMap.class);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", createArticleRequestList.size());
        response(successMap(data));
    }

    private Map<String, Object> pageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("dark", isDarkMode());
        data.put("colorPrimary", getAdminColorPrimary());
        data.put("plugin", session.getPlugin());
        return successMap(data);
    }

    private Map<String, Object> params() {
        if (requestInfo.getParam() == null) {
            return new HashMap<>();
        }
        return requestInfo.simpleParam();
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(ContentType.JSON, map, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            return String.valueOf(((List<?>) value).get(0));
        }
        return String.valueOf(value);
    }

    private boolean isDarkMode() {
        return requestInfo.isDarkMode();
    }

    private String getAdminColorPrimary() {
        return requestInfo.getAdminColorPrimary();
    }
}
