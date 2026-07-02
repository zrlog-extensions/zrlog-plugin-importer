package com.zrlog.plugin.importer.controller;

public class ImporterRequestParams {

    private String source;

    public static ImporterRequestParams of(String source) {
        ImporterRequestParams request = new ImporterRequestParams();
        request.setSource(source == null ? "" : source);
        return request;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
