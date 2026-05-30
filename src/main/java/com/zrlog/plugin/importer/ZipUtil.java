package com.zrlog.plugin.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUtil {
    public static void unZip(String src, String target) throws IOException {
        ZipEntry in;
        if (!target.endsWith("/")) {
            target = target + "/";
        }
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(src)); ZipFile zip = new ZipFile(src)) {
            //FIXME zip 文件不能有中文
            while ((in = zipIn.getNextEntry()) != null) {
                File file = new File(target + in.getName());
                if (in.getName().endsWith("/")) {
                    file.mkdirs();
                } else {
                    if (!new File(file.getParent()).exists()) {
                        new File(file.getParent()).mkdirs();
                    }
                    try (FileOutputStream fout = new FileOutputStream(file)) {
                        zip.getInputStream(in).transferTo(fout);
                    }
                }
            }
        }
    }
}
