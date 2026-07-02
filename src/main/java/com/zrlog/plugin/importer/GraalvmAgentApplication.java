package com.zrlog.plugin.importer;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.type.RunType;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.importer.controller.ImporterApiResponse;
import com.zrlog.plugin.importer.controller.ImporterController;
import com.zrlog.plugin.importer.controller.ImporterImportResponse;
import com.zrlog.plugin.importer.controller.ImporterPageData;
import com.zrlog.plugin.importer.controller.ImporterRequestParams;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        RunConstants.runType = RunType.AGENT;
        String basePath = System.getProperty("user.dir").replace("\\target","").replace("/target", "");
        //PathKit.setRootPath(basePath);
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath()  + "/", "/");
        //Application.nativeAgent = true;
        PluginNativeImageUtils.exposeController(Collections.singletonList(ImporterController.class));
        PluginNativeImageUtils.usedGsonObject();
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(ImporterApiResponse.class,
                ImporterImportResponse.class, ImporterPageData.class, ImporterRequestParams.class));
        Application.main(args);

    }
}
