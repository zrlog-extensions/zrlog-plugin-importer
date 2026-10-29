package com.zrlog.plugin.importer;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.HttpClientUtils;
import com.zrlog.plugin.common.type.HttpMethod;
import com.zrlog.plugin.data.codec.BaseHttpRequestInfo;
import com.zrlog.plugin.data.codec.HttpResponseInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads host-owned resources; callers cannot choose an outbound origin or redirect credentials. */
public final class AdminResourceSource {
    public static final long MAX_ARCHIVE_BYTES = 64L * 1024 * 1024;
    private final URI home;
    private final IOSession session;

    public AdminResourceSource(String apiHomeUrl, IOSession session) {
        this.session = session;
        home = URI.create(apiHomeUrl.replaceAll("/+$", ""));
        if (home.getHost() == null || home.getUserInfo() != null || home.getQuery() != null
                || home.getFragment() != null || !("http".equals(home.getScheme()) || "https".equals(home.getScheme()))) {
            throw new IllegalArgumentException("无法读取站点资源地址");
        }
    }

    public String contextPath() { return home.getPath(); }

    public URI resolve(String source) {
        URI value = URI.create(source);
        if (value.isAbsolute() || value.getRawAuthority() != null || value.getQuery() != null
                || value.getFragment() != null || source.contains("%") || source.contains("\\")
                || source.contains("//") || !value.normalize().equals(value)) {
            throw new IllegalArgumentException("请选择本站资源库中的 ZIP 文件");
        }
        String path = value.getPath();
        if (!contextPath().isEmpty() && path.startsWith(contextPath() + "/")) {
            path = path.substring(contextPath().length());
        }
        if (!(path.startsWith("/admin/attached/tmp/") || path.startsWith("/attached/"))
                || !path.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("请选择本站资源库中的 ZIP 文件");
        }
        return path.startsWith("/admin/attached/tmp/")
                ? URI.create(home + "/api/admin/upload/resource?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8))
                : URI.create(home.toString() + path);
    }

    public void download(String source, Map<String, String> credentials, Path destination) throws IOException {
        boolean complete = false;
        try {
            URI url = resolve(source);
            BaseHttpRequestInfo request = new BaseHttpRequestInfo();
            request.setAccessUrl(url.toString());
            request.setHttpMethod(HttpMethod.GET);
            Map<String, String> headers = new LinkedHashMap<>();
            if (url.getPath().equals(contextPath() + "/api/admin/upload/resource") && credentials != null) {
                credentials.forEach((key, value) -> {
                    if (value != null && ("Cookie".equalsIgnoreCase(key) || "Authorization".equalsIgnoreCase(key))) {
                        headers.put(key, value);
                    }
                });
            }
            request.setHeader(headers);
            HttpResponseInfo response;
            try {
                response = HttpClientUtils.sendHttpRequest(request, session, Duration.ofSeconds(70));
            } catch (RuntimeException e) {
                throw new IOException("资源下载失败，请重试", e);
            }
            if (response == null || response.getResponseBody() == null) {
                throw new IOException("资源下载失败，未收到文件内容");
            }
            if (response.getStatusCode() != 200) {
                throw new IOException("资源下载失败（HTTP " + response.getStatusCode() + "）");
            }
            if (response.getResponseBody().length > MAX_ARCHIVE_BYTES) {
                throw new IOException("ZIP 文件不能超过 64 MiB");
            }
            Files.write(destination, response.getResponseBody());
            complete = true;
        } finally {
            if (!complete) Files.deleteIfExists(destination);
        }
    }
}
