package cn.enaium.sma.utils;

import java.io.*;

/**
 * Project: SrgMappingAnalyze
 * Author: Enaium
 */
public class FileUtils {
    public static String read(String path) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(new File(path), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void write(String path, String txt) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(new File(path), txt, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
