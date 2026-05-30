package com.zrlog.plugin.importer.convertor;


import com.zrlog.plugin.common.model.CreateArticleRequest;

import java.io.File;

public interface ArticleConverter {

    CreateArticleRequest parse(File file) throws Exception;
}
