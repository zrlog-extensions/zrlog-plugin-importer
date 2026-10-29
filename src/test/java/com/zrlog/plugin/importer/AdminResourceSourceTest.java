package com.zrlog.plugin.importer;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.ClientActionHandler;
import com.zrlog.plugin.common.type.HttpMethod;
import com.zrlog.plugin.data.codec.BaseHttpRequestInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpResponseInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.data.codec.SocketCodec;
import com.zrlog.plugin.data.codec.SocketDecode;
import com.zrlog.plugin.data.codec.SocketEncode;
import com.zrlog.plugin.type.ActionType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AdminResourceSourceTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    /** Runs the same transport regression in a Native Image with no URL protocol flags. */
    public static void main(String[] args) throws Exception {
        AdminResourceSourceTest test = new AdminResourceSourceTest();
        test.temporary.create();
        try {
            test.rejectsExternalOriginsTraversalAndNonArchiveResources();
            test.downloadsBinaryThroughSharedHttpMethodAndScopesCredentialsToPrivateResources();
            test.rejectsRedirectAndServerErrorsWithoutRetainingFilesOrFollowingLocation();
            test.rejectsMissingResponseBodyAndCleansDestination();
            System.out.println("Shared HTTP download regression passed");
        } finally {
            test.temporary.delete();
        }
        // IOSession's shared idle scheduler is process scoped.
        System.exit(0);
    }

    @Test public void rejectsExternalOriginsTraversalAndNonArchiveResources() {
        var source = new AdminResourceSource("http://localhost:7080/blog/", null);
        assertEquals("/blog", source.contextPath());
        assertEquals("http://localhost:7080/blog/api/admin/upload/resource?path=%2Fadmin%2Fattached%2Ftmp%2Fhexo%2Fa.zip", source.resolve("/admin/attached/tmp/hexo/a.zip").toString());
        assertEquals("http://localhost:7080/blog/attached/a.zip", source.resolve("/blog/attached/a.zip").toString());
        for (String invalid : new String[]{"https://evil/a.zip", "//evil/a.zip", "/admin/attached/tmp/../a.zip",
                "/attached/%2e%2e/a.zip", "/attached/a.zip#x", "/attached/a.zip?x", "/attached//a.zip", "/admin/settings.zip", "/attached/a.exe"}) {
            assertThrows(invalid, IllegalArgumentException.class, () -> source.resolve(invalid));
        }
    }

    @Test public void downloadsBinaryThroughSharedHttpMethodAndScopesCredentialsToPrivateResources() throws Exception {
        // Includes invalid UTF-8: a ZIP must never be decoded as text or parsed as JSON.
        byte[] archive = new byte[5 * 1024 * 1024];
        archive[0] = 0x50; archive[1] = 0x4b; archive[archive.length - 1] = (byte) 0xff;
        try (SharedHttpSession host = new SharedHttpSession(200, archive)) {
            var source = new AdminResourceSource("http://host.invalid/blog/", host.session);
            var target = temporary.newFile().toPath();
            source.download("/admin/attached/tmp/a.zip", Map.of("cookie", "private",
                    "AUTHORIZATION", "Bearer private", "Host", "evil", "X-Other", "ignore"), target);
            assertArrayEquals(archive, Files.readAllBytes(target));
            BaseHttpRequestInfo request = host.requests.get(0);
            assertEquals(HttpMethod.GET, request.getHttpMethod());
            assertTrue(request.isResponseBodyAsFile());
            assertEquals(Long.valueOf(64L * 1024 * 1024), request.getMaxResponseBodyBytes());
            assertEquals("http://host.invalid/blog/api/admin/upload/resource?path=%2Fadmin%2Fattached%2Ftmp%2Fa.zip", request.getAccessUrl());
            assertEquals(Map.of("cookie", "private", "AUTHORIZATION", "Bearer private"), request.getHeader());
            source.download("/blog/attached/public.zip", Map.of("Cookie", "private", "Authorization", "private"), target);
            assertEquals("http://host.invalid/blog/attached/public.zip", host.requests.get(1).getAccessUrl());
            assertTrue(host.requests.get(1).getHeader().isEmpty());
            assertTrue(host.session.getPipeMap().isEmpty());
        }
    }

    @Test public void rejectsRedirectAndServerErrorsWithoutRetainingFilesOrFollowingLocation() throws Exception {
        for (int status : new int[]{302, 307, 401, 500}) {
            try (SharedHttpSession host = new SharedHttpSession(status, "error".getBytes())) {
                host.response.setHeader(Map.of("Location", "https://external.invalid/leak"));
                var source = new AdminResourceSource("http://host.invalid", host.session);
                var target = temporary.newFile().toPath();
                IOException error = assertThrows(IOException.class,
                        () -> source.download("/admin/attached/tmp/a.zip", Map.of("Cookie", "private"), target));
                assertTrue(error.getMessage().contains("HTTP " + status));
                assertEquals(1, host.requests.size());
                assertFalse(Files.exists(target));
                assertTrue(host.session.getPipeMap().isEmpty());
            }
        }
    }

    @Test public void rejectsMissingResponseBodyAndCleansDestination() throws Exception {
        try (SharedHttpSession host = new SharedHttpSession(200, null)) {
            var target = temporary.newFile().toPath();
            assertThrows(IOException.class, () -> new AdminResourceSource("http://host.invalid", host.session)
                    .download("/attached/a.zip", null, target));
            assertFalse(Files.exists(target));
        }
    }

    /** Exercises the published HttpClientUtils and IOSession response lease, with a core transport fixture. */
    private static final class SharedHttpSession implements AutoCloseable {
        private final ServerSocketChannel server = ServerSocketChannel.open();
        private final SocketChannel sender;
        private final Selector selector = Selector.open();
        private final IOSession session;
        private final List<BaseHttpRequestInfo> requests = new ArrayList<>();
        private final HttpResponseInfo response = new HttpResponseInfo();
        private final List<java.nio.file.Path> files = new ArrayList<>();

        private SharedHttpSession(int status, byte[] body) throws IOException {
            response.setStatusCode(status);
            response.setResponseBody(body);
            response.setHeader(Map.of());
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            sender = SocketChannel.open(server.getLocalAddress());
            SocketChannel receiver = server.accept();
            SocketEncode transport = new SocketEncode() {
                @Override public void doEncode(IOSession session, MsgPacket packet) {
                    assertEquals(ActionType.HTTP_METHOD.name(), packet.getMethodStr());
                    assertEquals(MsgPacketStatus.SEND_REQUEST, packet.getStatus());
                    requests.add(new Gson().fromJson(packet.getDataStr(), BaseHttpRequestInfo.class));
                    if (response.getStatusCode() == 200 && response.getResponseBody() != null) {
                        try {
                            var file = Files.createTempFile("hexo-http-fixture-", ".zip");
                            files.add(file);
                            Files.write(file, response.getResponseBody());
                            session.dispose(new MsgPacket(file.toFile(), ContentType.FILE, MsgPacketStatus.RESPONSE_SUCCESS,
                                    packet.getMsgId(), packet.getMethodStr()));
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    } else {
                        session.dispose(new MsgPacket(response, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS,
                                packet.getMsgId(), packet.getMethodStr()));
                    }
                }
            };
            session = new IOSession(receiver, selector, new SocketCodec(transport, new SocketDecode(Runnable::run)),
                    new ClientActionHandler());
        }

        @Override public void close() throws IOException {
            session.close();
            sender.close();
            server.close();
            selector.close();
            for (var file : files) Files.deleteIfExists(file);
        }
    }
}
