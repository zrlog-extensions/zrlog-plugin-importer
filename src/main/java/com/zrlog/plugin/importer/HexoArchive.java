package com.zrlog.plugin.importer;

import com.zrlog.plugin.common.model.CreateArticleRequest;
import com.zrlog.plugin.importer.convertor.HexoConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class HexoArchive {
    private HexoArchive() {}

    public static List<CreateArticleRequest> read(Path zip, Path directory) throws IOException {
        ZipUtil.unZip(zip.toString(), directory.toString());
        List<CreateArticleRequest> articles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile).sorted()::iterator) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!(name.endsWith(".md") || name.endsWith(".markdown"))) continue;
                try {
                    CreateArticleRequest article = new HexoConverter().parse(path.toFile());
                    if (article == null) continue;
                    if (article.getType() == null || article.getType().isEmpty()) article.setType("未分类");
                    articles.add(article);
                } catch (ParseException | RuntimeException e) {
                    throw new IOException("无法读取文章：" + path.getFileName(), e);
                }
            }
        }
        articles.sort(Comparator.comparing(CreateArticleRequest::getReleaseDate));
        return articles;
    }
}
