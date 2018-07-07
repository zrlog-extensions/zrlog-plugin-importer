package com.zrlog.plugin.importer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ParseUtil {

    public static String autoDigest(String str, int size) {
        StringBuilder sb = new StringBuilder();
        Document document = Jsoup.parseBodyFragment(str);
        List<Node> allTextNode = new ArrayList<>();
        getAllTextNode(document.childNodes(), allTextNode);
        int tLength = 0;
        for (Node node : allTextNode) {
            if (node instanceof TextNode) {
                sb.append(node.parent().outerHtml());
                tLength += ((TextNode) node).text().length();
                if (tLength > size) {
                    sb.append(" ...");
                    break;
                }
            }
        }
        String digest = sb.toString();
        Elements elements = Jsoup.parse(str).body().select("video");
        if (elements != null && !elements.isEmpty()) {
            digest = elements.get(0).toString() + "<br/>" + digest;
        }
        return digest.trim();
    }

    private static void getAllTextNode(List<Node> nodes, List<Node> nodeList) {
        for (Node node : nodes) {
            if (!node.childNodes().isEmpty()) {
                getAllTextNode(node.childNodes(), nodeList);
            } else {
                if (node instanceof TextNode) {
                    if (((TextNode) node).text().trim().length() > 0) {
                        nodeList.add(node);
                    }
                }
            }
        }
    }
}
