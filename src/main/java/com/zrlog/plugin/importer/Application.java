package com.zrlog.plugin.importer;

import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.importer.controller.ImporterController;
import com.zrlog.plugin.render.FreeMarkerRenderHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(ImporterController.class);
        new NioClient(null, new FreeMarkerRenderHandler()).connectServer(args, classList, ImporterPlugin.class);
    }
}
