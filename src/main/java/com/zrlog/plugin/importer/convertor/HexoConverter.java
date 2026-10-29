package com.zrlog.plugin.importer.convertor;

import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.model.CreateArticleRequest;
import com.zrlog.plugin.importer.ParseUtil;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HexoConverter implements ArticleConverter {

    private static List<Extension> extensions = Arrays.asList(TablesExtension.create(), AutolinkExtension.create());

    public CreateArticleRequest parse(File file) throws IOException, ParseException {
        String mdContent;
        try (FileInputStream input = new FileInputStream(file)) {
            mdContent = IOUtil.getStringInputStream(input).replace("\r\n", "\n");
        }
        if (mdContent.startsWith("---\n")) {
            mdContent = mdContent.substring("---\n".length());
        }
        if (mdContent.startsWith("---\r\n")) {
            mdContent = mdContent.substring("---\r\n".length());
        }
        String[] texts = mdContent.split("---\n");
        if (texts.length > 1) {
            String metaInfo = texts[0].replace("\t", " ").trim();
            String articleMd = mdContent.substring(mdContent.indexOf("---\n") + 4);
            Yaml yaml = new Yaml();
            Map<String, Object> metaInfoMap = yaml.load(metaInfo);
            CreateArticleRequest createArticleRequest = new CreateArticleRequest();
            Object categories = metaInfoMap.get("categories");
            if (categories instanceof List && !((List<?>) categories).isEmpty()) {
                createArticleRequest.setType(((List) categories).get(0).toString());
            } else if (categories instanceof String) {
                createArticleRequest.setType(categories.toString());
            }
            Object tags = metaInfoMap.get("tags");
            if (tags instanceof List) {
                StringJoiner stringJoiner = new StringJoiner(",");
                for (Object tag : (List) tags) {
                    stringJoiner.add(tag.toString());
                }
                createArticleRequest.setKeywords(stringJoiner.toString());
            } else if (tags instanceof String) {
                createArticleRequest.setKeywords((String) tags);
            }

            Object date = metaInfoMap.get("date");
            if (date == null || (date instanceof String && ((String) date).trim().isEmpty())) {
                createArticleRequest.setReleaseDate(new Date(Files.getLastModifiedTime(file.toPath()).toMillis()));
            } else if (date instanceof Date) {
                createArticleRequest.setReleaseDate((Date) date);
            } else if (date instanceof String) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    createArticleRequest.setReleaseDate(sdf.parse((String) date));
                } catch (ParseException e) {
                    ParseException error = new ParseException("date 不是有效的日期", e.getErrorOffset());
                    error.initCause(e);
                    throw error;
                }
            } else {
                throw new ParseException("date 必须为日期字符串", 0);
            }
            createArticleRequest.setTitle((String) metaInfoMap.get("title"));
            createArticleRequest.setMarkdown(articleMd);
            createArticleRequest.setAlias(file.getName().replace(".md", "").replace(".markdown", ""));
            createArticleRequest.setContent(renderMd(createArticleRequest.getMarkdown()));
            createArticleRequest.setThumbnail(getThumbnail(metaInfoMap));
            int idx = createArticleRequest.getMarkdown().indexOf("<!--more-->");
            if (idx > -1) {
                createArticleRequest.setDigest(renderMd(createArticleRequest.getMarkdown().substring(0, idx)));
            } else {
                createArticleRequest.setDigest(ParseUtil.autoDigest(createArticleRequest.getContent(), 200));
            }
            return createArticleRequest;
        }
        return null;
    }

    private String getThumbnail(Map<String, Object> meta) {
        String[] keys = {"photos", "header-img", "thumbnail"};
        for (String key : keys) {
            if (meta.get(key) != null) {
                return meta.get(key).toString();
            }
        }
        return "";
    }

    private static String renderMd(String md) {
        Parser parser = Parser.builder()
                .extensions(extensions)
                .build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
        return renderer.render(document);
    }

    public static void main(String[] args) throws Exception {
        new HexoConverter().parse(new File("_posts/Docker.md"));
    }


}
