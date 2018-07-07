package com.zrlog.plugin.importer.convertor;

import com.hibegin.common.util.IOUtil;

import com.zrlog.plugin.common.modle.CreateArticleRequest;
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
import java.io.FileNotFoundException;
import java.util.*;

public class HexoConverter implements ArticleConverter {

    private static List<Extension> extensions = Arrays.asList(TablesExtension.create(), AutolinkExtension.create());

    public CreateArticleRequest parse(File file) throws FileNotFoundException {
        String mdContent = IOUtil.getStringInputStream(new FileInputStream(file.toString()));
        if (mdContent.startsWith("---\n")) {
            mdContent = mdContent.substring("---\n".length());
        }
        if (mdContent.startsWith("---\r\n")) {
            mdContent = mdContent.substring("---\r\n".length());
        }
        String[] texts = mdContent.split("---");
        if (texts.length > 1) {
            String metaInfo = texts[0].replace("\t", " ").trim();
            Yaml yaml = new Yaml();
            Map<String, Object> metaInfoMap = yaml.load(metaInfo);
            CreateArticleRequest createArticleRequest = new CreateArticleRequest();
            Object categories = metaInfoMap.get("categories");
            if (categories instanceof List) {
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

            createArticleRequest.setReleaseDate((Date) metaInfoMap.get("date"));
            createArticleRequest.setTitle((String) metaInfoMap.get("title"));
            createArticleRequest.setMarkdown(texts[1].trim());
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

    public static void main(String[] args) throws FileNotFoundException {
        new HexoConverter().parse(new File("_posts/js-memory-management-and-gc.md"));
    }


}
