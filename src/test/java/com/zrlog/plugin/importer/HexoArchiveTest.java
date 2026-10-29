package com.zrlog.plugin.importer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class HexoArchiveTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    /** Runs archive parsing in Native Image using the same resource metadata as the plugin. */
    public static void main(String[] args) throws Exception {
        HexoArchiveTest test = new HexoArchiveTest();
        test.temporary.create();
        try {
            test.readsPublicRepositoryPostsInsideNestedHexoDirectoryAndSortsDates();
            test.acceptsWindowsNewlinesAndEmptyCategories();
            test.rejectsZipTraversalAndExpansionBeyondPerFileLimit();
            test.importsOfficialStarterWithoutDateUsingArchiveModificationTime();
            test.sortsDatedAndEmptyDateArticlesUsingArchiveTimes();
            test.reportsInvalidExplicitDateWithTheArticleName();
            System.out.println("Hexo archive parsing regression passed");
        } finally {
            test.temporary.delete();
        }
    }

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

    @Test public void importsOfficialStarterWithoutDateUsingArchiveModificationTime() throws Exception {
        // Unmodified hexojs/hexo-starter hello-world.md at c92a4154cd5a30e5d33b09f7e5fb629ced6100d5.
        String starter = Files.readString(Path.of("src/test/resources/hexo/hello-world-2.md"));
        FileTime modified = FileTime.from(Instant.parse("2020-06-15T10:20:30Z"));
        Path zip = temporary.newFile("starter.zip").toPath();
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            ZipEntry entry = new ZipEntry("source/_posts/hello-world-2.md");
            entry.setLastModifiedTime(modified);
            output.putNextEntry(entry);
            output.write(starter.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        var articles = HexoArchive.read(zip, temporary.newFolder().toPath());
        assertEquals(1, articles.size());
        assertEquals("Hello World", articles.get(0).getTitle());
        assertEquals("hello-world-2", articles.get(0).getAlias());
        assertEquals(modified.toMillis(), articles.get(0).getReleaseDate().getTime());
        assertTrue(articles.get(0).getContent().contains("https://hexo.io/"));
        assertEquals(articles.get(0).getReleaseDate(),
                HexoArchive.read(zip, temporary.newFolder().toPath()).get(0).getReleaseDate());
    }

    @Test public void reportsInvalidExplicitDateWithTheArticleName() throws Exception {
        for (String date : new String[]{"not-a-date", "1234"}) {
            Path zip = archive("source/_posts/hello-world-2.md", "---\ntitle: Test\ndate: " + date + "\n---\nBody");
            IOException error = assertThrows(IOException.class,
                    () -> HexoArchive.read(zip, temporary.newFolder().toPath()));
            assertTrue(error.getMessage(), error.getMessage().contains("hello-world-2.md"));
            assertTrue(error.getMessage(), error.getMessage().contains("date"));
        }
    }

    @Test public void sortsDatedAndEmptyDateArticlesUsingArchiveTimes() throws Exception {
        Path zip = temporary.newFile().toPath();
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String name : new String[]{"a-explicit-date.md", "z-empty-date.md"}) {
                ZipEntry entry = new ZipEntry("source/_posts/" + name);
                entry.setLastModifiedTime(FileTime.from(Instant.parse("2020-06-15T10:20:30Z")));
                output.putNextEntry(entry);
                String date = name.startsWith("a-") ? "2024-01-01 12:00:00" : "";
                output.write(("---\ntitle: Test\ndate: " + date + "\n---\nBody").getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        var articles = HexoArchive.read(zip, temporary.newFolder().toPath());
        assertEquals(2, articles.size());
        assertEquals("z-empty-date", articles.get(0).getAlias());
        assertEquals("a-explicit-date", articles.get(1).getAlias());
        assertTrue(articles.get(0).getReleaseDate().before(articles.get(1).getReleaseDate()));
    }

    private Path archive(String entry, String content) throws Exception {
        Path zip = temporary.newFile().toPath();
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry(entry)); output.write(content.getBytes(StandardCharsets.UTF_8)); output.closeEntry();
        }
        return zip;
    }
}
