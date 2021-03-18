package cn.enaium.sma;

import java.util.HashMap;

/**
 * Project: SrgMappingAnalyze
 * Author: Enaium
 */
public enum SMA {

    INSTANCE;

    public final HashMap<String, String> classObfToCleanMap = new HashMap<>();
    public final HashMap<String, String> classCleanToObfMap = new HashMap<>();

    public final HashMap<String, String> fieldObfToCleanMap = new HashMap<>();
    public final HashMap<String, String> fieldCleanToObfMap = new HashMap<>();

    public final HashMap<String, String> methodObfToCleanMap = new HashMap<>();
    public final HashMap<String, String> methodCleanToObfMap = new HashMap<>();

    public void parseMapping(String text) {
        String[] lines = text.split("\\r\\n|\\n");
        int line = 0;
        for (String string : lines) {
            line++;
            String[] args = string.trim().split(" ");
            String type = args[0];
            try {
                switch (type) {
                    case "CL:": {
                        String obf = args[1];
                        String clean = args[2];
                        classObfToCleanMap.put(obf, clean);
                        classCleanToObfMap.put(clean, obf);
                        break;
                    }
                    case "FD:": {
                        String obf = args[1];
                        String clean = args[2];
                        fieldObfToCleanMap.put(obf, clean);
                        fieldCleanToObfMap.put(clean, obf);
                        break;
                    }
                    case "MD:": {
                        String obf = args[1];
                        String obfDescription = args[2];
                        String clean = args[3];
                        String cleanDescription = args[4];
                        methodObfToCleanMap.put(obf + " " + obfDescription, clean + " " + cleanDescription);
                        methodCleanToObfMap.put(clean + " " + cleanDescription, obf + " " + obfDescription);
                        break;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
