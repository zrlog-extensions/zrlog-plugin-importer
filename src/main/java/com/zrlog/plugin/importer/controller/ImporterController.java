package com.zrlog.plugin.importer.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.importer.AdminResourceSource;
import com.zrlog.plugin.importer.HexoArchive;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.CreateArticleRequest;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
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

    public void index() throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", isDarkMode() ? "dark" : "light");
        data.put("data", gson.toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());

    }

    public void json() throws IOException, InterruptedException {
        response(pageData());
    }

    public void doImport() throws IOException {
        String source = params().getSource();
        if (source.trim().isEmpty()) {
            response(ImporterApiResponse.error("请先上传 zip 文件"));
            return;
        }
        Path tmpPath = Files.createTempDirectory("zrlog-hexo-");
        try {
            Path zip = tmpPath.resolve("importer.zip");
            resourceSource().download(source, requestInfo.getHeader(), zip);
            List<CreateArticleRequest> articles = HexoArchive.read(zip, tmpPath.resolve("posts"));
            if (articles.isEmpty()) {
                response(ImporterApiResponse.error("ZIP 中没有可导入的 Hexo 文章"));
                return;
            }
            for (CreateArticleRequest article : articles) {
                article.setUserId(requestInfo.getUserId());
                session.getResponseSync(ContentType.JSON, article, ActionType.CREATE_ARTICLE, Object.class);
            }
            response(ImporterApiResponse.success(new ImporterImportResponse(articles.size())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response(ImporterApiResponse.error("导入已中断，请重试"));
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Hexo import failed", e);
            response(ImporterApiResponse.error(e.getMessage()));
        } finally {
            try (Stream<Path> paths = Files.walk(tmpPath)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private AdminResourceSource resourceSource() throws IOException, InterruptedException {
        PublicInfo host = session.getResponseSync(ContentType.JSON, Collections.emptyMap(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
        if (host == null || host.getApiHomeUrl() == null) throw new IOException("无法读取站点资源地址");
        return new AdminResourceSource(host.getApiHomeUrl());
    }

    private ImporterApiResponse<ImporterPageData> pageData() throws IOException, InterruptedException {
        ImporterPageData data = new ImporterPageData();
        data.setContextPath(resourceSource().contextPath());
        data.setDark(isDarkMode());
        data.setColorPrimary(getAdminColorPrimary());
        data.setPlugin(session.getPlugin());
        return ImporterApiResponse.success(data);
    }

    private ImporterRequestParams params() {
        return ImporterRequestParams.of(paramValue("source"));
    }

    private void response(ImporterApiResponse<?> response) {
        session.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private String paramValue(String key) {
        if (requestInfo.getParam() == null || requestInfo.getParam().get(key) == null || requestInfo.getParam().get(key).length == 0) {
            return "";
        }
        return requestInfo.getParam().get(key)[0];
    }

    private boolean isDarkMode() {
        return requestInfo.isDarkMode();
    }

    private String getAdminColorPrimary() {
        return requestInfo.getAdminColorPrimary();
    }
}
