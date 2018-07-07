package com.zrlog.plugin.importer.controller;

import com.hibegin.common.util.StringUtils;
import com.hibegin.common.util.ZipUtil;
import com.hibegin.common.util.http.HttpUtil;
import com.hibegin.common.util.http.handle.HttpFileHandle;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.PathKit;
import com.zrlog.plugin.common.modle.CreateArticleRequest;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.importer.convertor.HexoConverter;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImporterController {

    private static final Logger LOGGER = LoggerUtil.getLogger(ImporterController.class);

    private IOSession session;
    private MsgPacket requestPacket;
    private HttpRequestInfo requestInfo;

    public ImporterController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void index() {
        Map<String, Object> keyMap = new HashMap<>();
        session.responseHtml("/templates/index.ftl", keyMap, requestPacket.getMethodStr(), requestPacket.getMsgId());

    }

    public void doImport() throws IOException {
        String source = requestInfo.getParam().get("source")[0];
        File tmpPath = new File(PathKit.getTmpPath() + "/" + UUID.randomUUID().toString() + "/");
        tmpPath.mkdirs();
        HttpFileHandle fileHandler = new HttpFileHandle(tmpPath.toString()) {
        };
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Cookie", (String) requestInfo.getHeader().get("Cookie"));
        HttpUtil.getDisableRedirectInstance().sendGetRequest(source, new HashMap<>(), fileHandler, requestHeaders);
        fileHandler.getT();
        ZipUtil.unZip(fileHandler.getT().toString(), tmpPath.toString() + "/");
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
                if (StringUtils.isEmpty(request.getType())) {
                    request.setType("未分类");
                }
                createArticleRequestList.add(request);
            }
        }
        createArticleRequestList.sort(Comparator.comparingLong(o -> o.getReleaseDate().getTime()));
        for (CreateArticleRequest request : createArticleRequestList) {
            session.getResponseSync(ContentType.JSON, request, ActionType.CREATE_ARTICLE, HashMap.class);
        }
        session.sendJsonMsg(new HashMap<>(), requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }
}
