package com.zrlog.plugin.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtil {
    public static void unZip(String src, String target) throws IOException {
        File targetDir = new File(target).getCanonicalFile();
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Create unzip target folder failed: " + targetDir);
        }
        try (ZipFile zip = new ZipFile(src)) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                File file = new File(targetDir, entry.getName()).getCanonicalFile();
                if (!file.toPath().startsWith(targetDir.toPath())) {
                    throw new IOException("Unsafe zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new IOException("Create unzip folder failed: " + file);
                    }
                    continue;
                }
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Create unzip folder failed: " + parent);
                }
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    zip.getInputStream(entry).transferTo(fout);
                }
            }
        }
    }
}
