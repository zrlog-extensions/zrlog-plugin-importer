package com.zrlog.plugin.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
            long total = 0;
            int count = 0;
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (++count > 10_000) throw new IOException("ZIP 文件条目过多");
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
                try (FileOutputStream fout = new FileOutputStream(file); InputStream input = zip.getInputStream(entry)) {
                    byte[] buffer = new byte[8192];
                    long entrySize = 0;
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        entrySize += read;
                        if (total > 128L * 1024 * 1024 || entrySize > 8L * 1024 * 1024) {
                            throw new IOException("ZIP 解压内容过大");
                        }
                        fout.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
