package com.zrlog.plugin.importer;

import com.sun.net.httpserver.HttpServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class AdminResourceSourceTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rejectsExternalOriginsTraversalAndNonArchiveResources() {
        var source = new AdminResourceSource("http://localhost:7080/blog/");
        assertEquals("/blog", source.contextPath());
        assertEquals("http://localhost:7080/blog/api/admin/upload/resource?path=%2Fadmin%2Fattached%2Ftmp%2Fhexo%2Fa.zip", source.resolve("/admin/attached/tmp/hexo/a.zip").toString());
        assertEquals("http://localhost:7080/blog/attached/a.zip", source.resolve("/blog/attached/a.zip").toString());
        for (String invalid : new String[]{"https://evil/a.zip", "//evil/a.zip", "/admin/attached/tmp/../a.zip",
                "/attached/%2e%2e/a.zip", "/attached/a.zip#x", "/attached/a.zip?x", "/attached//a.zip", "/admin/settings.zip", "/attached/a.exe"}) {
            assertThrows(invalid, IllegalArgumentException.class, () -> source.resolve(invalid));
        }
    }

    @Test public void streamsBeyondProtocolLimitAndNeverFollowsRedirectsOrSendsPublicCredentials() throws Exception {
        HttpServer host = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] data = new byte[5 * 1024 * 1024];
        data[data.length - 1] = 12;
        AtomicReference<String> cookie = new AtomicReference<>();
        AtomicInteger redirected = new AtomicInteger();
        host.createContext("/api/admin/upload/resource", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            exchange.sendResponseHeaders(200, data.length);
            try (var output = exchange.getResponseBody()) {output.write(data);}
        });
        host.createContext("/attached/public.zip", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            exchange.getResponseHeaders().add("Location", "/leak");
            exchange.sendResponseHeaders(302, -1); exchange.close();
        });
        host.createContext("/leak", exchange -> {redirected.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close();});
        host.start();
        var target = temporary.newFile().toPath();
        try {
            var source = new AdminResourceSource("http://127.0.0.1:" + host.getAddress().getPort());
            source.download("/admin/attached/tmp/a.zip", Map.of("cookie", "private"), target);
            assertArrayEquals(data, Files.readAllBytes(target));
            assertEquals("private", cookie.get());
            assertThrows(IOException.class, () -> source.download("/attached/public.zip", Map.of("Cookie", "private"), target));
            assertNull(cookie.get());
            assertFalse(Files.exists(target));
            assertEquals(0, redirected.get());
        } finally {host.stop(0);}
    }
}
