package com.zrlog.plugin.importer.convertor;

import com.zrlog.plugin.common.modle.CreateArticleRequest;

import java.io.File;

public interface ArticleConverter {

    CreateArticleRequest parse(File file) throws Exception;
}
