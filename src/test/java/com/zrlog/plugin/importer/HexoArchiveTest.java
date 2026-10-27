package com.zrlog.plugin.importer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class HexoArchiveTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void readsPublicRepositoryPostsInsideNestedHexoDirectoryAndSortsDates() throws Exception {
        Path zip = temporary.newFile("hexo.zip").toPath();
        try (var output = new ZipOutputStream(Files.newOutputStream(zip)); var samples = Files.list(Path.of("_posts"))) {
            for (Path sample : (Iterable<Path>) samples.filter(path -> path.toString().endsWith(".md"))::iterator) {
                output.putNextEntry(new ZipEntry("hexo-main/source/_posts/" + sample.getFileName()));
                Files.copy(sample, output); output.closeEntry();
            }
        }
        var articles = HexoArchive.read(zip, temporary.newFolder().toPath());
        assertEquals(4, articles.size());
        for (int i = 0; i < articles.size(); i++) {
            assertNotNull(articles.get(i).getTitle());
            assertFalse(articles.get(i).getMarkdown().isEmpty());
            if (i > 0) assertFalse(articles.get(i).getReleaseDate().before(articles.get(i - 1).getReleaseDate()));
        }
    }

    @Test public void acceptsWindowsNewlinesAndEmptyCategories() throws Exception {
        Path zip = archive("_posts/post.md", "---\r\ntitle: Test\r\ndate: 2026-10-27 12:00:00\r\ncategories: []\r\n---\r\nBody");
        var articles = HexoArchive.read(zip, temporary.newFolder().toPath());
        assertEquals(1, articles.size());
        assertEquals("未分类", articles.get(0).getType());
    }

    @Test public void rejectsZipTraversalAndExpansionBeyondPerFileLimit() throws Exception {
        assertThrows(IOException.class, () -> HexoArchive.read(archive("../outside.md", "bad"), temporary.newFolder().toPath()));
        assertThrows(IOException.class, () -> HexoArchive.read(archive("huge.md", "x".repeat(8 * 1024 * 1024 + 1)), temporary.newFolder().toPath()));
    }

    private Path archive(String entry, String content) throws Exception {
        Path zip = temporary.newFile().toPath();
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry(entry)); output.write(content.getBytes(StandardCharsets.UTF_8)); output.closeEntry();
        }
        return zip;
    }
}
