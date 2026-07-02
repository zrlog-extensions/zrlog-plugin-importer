package com.zrlog.plugin.importer.controller;

public class ImporterApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ImporterApiResponse() {
    }

    private ImporterApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ImporterApiResponse<T> success(T data) {
        return new ImporterApiResponse<T>(true, null, data);
    }

    public static ImporterApiResponse<Void> error(String message) {
        return new ImporterApiResponse<Void>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
