package com.zrlog.plugin.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Reads host-owned resources; callers cannot choose an outbound origin or redirect credentials. */
public final class AdminResourceSource {
    public static final long MAX_ARCHIVE_BYTES = 64L * 1024 * 1024;
    private final URI home;

    public AdminResourceSource(String apiHomeUrl) {
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
        URI url = resolve(source);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(60_000);
        if (url.getPath().equals(contextPath() + "/api/admin/upload/resource") && credentials != null) {
            credentials.forEach((key, value) -> {
                if (value != null && ("Cookie".equalsIgnoreCase(key) || "Authorization".equalsIgnoreCase(key))) {
                    connection.setRequestProperty(key, value);
                }
            });
        }
        boolean complete = false;
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("资源下载失败（HTTP " + status + "）");
            if (connection.getContentLengthLong() > MAX_ARCHIVE_BYTES) throw new IOException("ZIP 文件不能超过 64 MiB");
            try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_ARCHIVE_BYTES) throw new IOException("ZIP 文件不能超过 64 MiB");
                    output.write(buffer, 0, count);
                }
            }
            complete = true;
        } finally {
            connection.disconnect();
            if (!complete) Files.deleteIfExists(destination);
        }
    }
}
