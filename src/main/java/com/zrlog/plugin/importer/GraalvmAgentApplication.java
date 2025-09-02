package com.zrlog.plugin.importer;

import com.google.gson.Gson;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.importer.controller.ImporterController;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.render.FreeMarkerRenderHandler;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        new Gson().toJson(new HttpRequestInfo());
        new Gson().toJson(new Plugin());
        //new Gson().toJson(new User());
        //new Gson().toJson(new CommentsEntry());
        String basePath = System.getProperty("user.dir").replace("\\target","").replace("/target", "");
        //PathKit.setRootPath(basePath);
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath()  + "/", "/");
        //Application.nativeAgent = true;
        PluginNativeImageUtils.exposeController(Collections.singletonList(ImporterController.class));
        PluginNativeImageUtils.usedGsonObject();

        //Application.nativeAgent = true;
        Plugin plugin = new Plugin();
        plugin.setName("test");
        plugin.setDesc("test");
        plugin.setVersion("test");
        Map<String, Object> objectObjectHashMap = new HashMap<>();
        new FreeMarkerRenderHandler().render("/templates/index", plugin, objectObjectHashMap);

        Application.main(args);

    }
}