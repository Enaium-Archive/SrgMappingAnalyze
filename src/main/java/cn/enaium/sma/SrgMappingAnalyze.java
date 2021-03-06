package cn.enaium.sma;

import cn.enaium.sma.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Project: SrgMappingAnalyze
 * Author: Enaium
 */
public class SrgMappingAnalyze {

    private static final HashMap<String, Object> nameToClass = new HashMap<>();
    public static final HashMap<String, String> map = new HashMap<>();

    private static final String NAME_LINE = "^.+:";
    private static final String SPLITTER = "( |->)+";

    public static void main(String[] args) throws IOException {
        System.out.println("          _____                    _____                    _____          \n" +
                "         /\\    \\                  /\\    \\                  /\\    \\         \n" +
                "        /::\\    \\                /::\\____\\                /::\\    \\        \n" +
                "       /::::\\    \\              /::::|   |               /::::\\    \\       \n" +
                "      /::::::\\    \\            /:::::|   |              /::::::\\    \\      \n" +
                "     /:::/\\:::\\    \\          /::::::|   |             /:::/\\:::\\    \\     \n" +
                "    /:::/__\\:::\\    \\        /:::/|::|   |            /:::/__\\:::\\    \\    \n" +
                "    \\:::\\   \\:::\\    \\      /:::/ |::|   |           /::::\\   \\:::\\    \\   \n" +
                "  ___\\:::\\   \\:::\\    \\    /:::/  |::|___|______    /::::::\\   \\:::\\    \\  \n" +
                " /\\   \\:::\\   \\:::\\    \\  /:::/   |::::::::\\    \\  /:::/\\:::\\   \\:::\\    \\ \n" +
                "/::\\   \\:::\\   \\:::\\____\\/:::/    |:::::::::\\____\\/:::/  \\:::\\   \\:::\\____\\\n" +
                "\\:::\\   \\:::\\   \\::/    /\\::/    /      /:::/    /\\::/    \\:::\\  /:::/    /\n" +
                " \\:::\\   \\:::\\   \\/____/  \\/____/      /:::/    /  \\/____/ \\:::\\/:::/    / \n" +
                "  \\:::\\   \\:::\\    \\                  /:::/    /            \\::::::/    /  \n" +
                "   \\:::\\   \\:::\\____\\                /:::/    /              \\::::/    /   \n" +
                "    \\:::\\  /:::/    /               /:::/    /               /:::/    /    \n" +
                "     \\:::\\/:::/    /               /:::/    /               /:::/    /     \n" +
                "      \\::::::/    /               /:::/    /               /:::/    /      \n" +
                "       \\::::/    /               /:::/    /               /:::/    /       \n" +
                "        \\::/    /                \\::/    /                \\::/    /        \n" +
                "         \\/____/                  \\/____/                  \\/____/         \n" +
                "\n");
        System.out.println("SMA (SrgMappingAnalyze) By:Enaium");


        program();
    }

    private static String mappingFile;
    public static String mappingSuffix;

    private static void inputMapping() {
        System.out.println("Input Srg Mapping:");
        String mappingPath = new Scanner(System.in).next();
        mappingFile = mappingPath.substring(0, mappingPath.lastIndexOf("."));
        mappingSuffix = mappingPath.substring(mappingPath.lastIndexOf(".") + 1);

        SMA.INSTANCE.parseMapping(Objects.requireNonNull(FileUtils.read(mappingPath)));

        if (SMA.INSTANCE.classObfToCleanMap.isEmpty() && SMA.INSTANCE.fieldObfToCleanMap.isEmpty() && SMA.INSTANCE.methodObfToCleanMap.isEmpty()) {
            System.out.println("Not srg mapping");
        }

        SMA.INSTANCE.classCleanToObfMap.forEach((k, v) -> nameToClass.put(k.substring(k.lastIndexOf("/") + 1), k));
    }

    private static void program() throws IOException {
        System.out.println("Features:");
        System.out.println("[0]:Mapping key to value,value to key");
        System.out.println("[1]:Proguard mapping to Srg mapping");
        System.out.println("[2]:DeObf jar");
        System.out.println("[3]:ReObf jar");
        System.out.println("[4]:Mixin refmap update");
        System.out.println("[5]:Mixin refmap builder");
        System.out.println("Input:");
        Scanner features = new Scanner(System.in);
        switch (features.nextInt()) {
            case 0: {
                inputMapping();
                StringBuilder stringBuilder = new StringBuilder();
                SMA.INSTANCE.classObfToCleanMap.forEach((k, v) -> stringBuilder.append("CL: ").append(v).append(" ").append(k).append("\n"));
                SMA.INSTANCE.fieldObfToCleanMap.forEach((k, v) -> stringBuilder.append("FD: ").append(v).append(" ").append(k).append("\n"));
                SMA.INSTANCE.methodObfToCleanMap.forEach((k, v) -> stringBuilder.append("MD: ").append(v).append(" ").append(k).append("\n"));
                FileUtils.write(mappingFile + "_obf." + mappingSuffix, stringBuilder.toString());
                break;
            }
            case 1: {
                System.out.println("Input Proguard Mapping:");
                String proguardPath = new Scanner(System.in).next();
                proguardToSrg(proguardPath);
                break;
            }
            case 2: {
                inputMapping();
                System.out.println("Input Jar:");
                String jarPath = new Scanner(System.in).next();
                System.out.println("DeObf Jar start");
                remapJar(jarPath, true);
                System.out.println("DeObf Jar end");
                break;
            }
            case 3: {
                inputMapping();
                System.out.println("Input Jar:");
                String jarPath = new Scanner(System.in).next();
                System.out.println("ReObf Jar start");
                remapJar(jarPath, false);
                System.out.println("ReObf Jar end");
                break;
            }
            case 4: {
                inputMapping();
                System.out.println("Warning: MixinName = clean name + Mixin(such as MinecraftMixin or MixinMinecraft)");
                System.out.println("Input refmap:");
                String refmapPath = new Scanner(System.in).next();
                String refmapFile = refmapPath.substring(0, refmapPath.lastIndexOf("."));
                String refmapSuffix = refmapPath.substring(refmapPath.lastIndexOf(".") + 1);

                JsonObject jsonObject = new Gson().fromJson(Objects.requireNonNull(FileUtils.read(refmapPath)), JsonObject.class);
                JsonObject mappings = jsonObject.getAsJsonObject("mappings");
                JsonObject newJsonObject = new JsonObject();
                JsonObject newJsonMappings = new JsonObject();
                for (Map.Entry<String, JsonElement> mappingsElement : mappings.entrySet()) {
                    String mixin = mappingsElement.getKey().substring(mappingsElement.getKey().lastIndexOf("/") + 1).replace("Mixin", "");
                    String type = (String) nameToClass.get(mixin);
                    JsonObject mapping = new Gson().fromJson(mappingsElement.getValue().toString(), JsonObject.class);
                    JsonObject newMapping = new JsonObject();
                    for (Map.Entry<String, JsonElement> mappingElement : mapping.entrySet()) {
                        //Method
                        if (mappingElement.getKey().contains("(")) {
                            String mappingName = mappingElement.getKey().substring(0, mappingElement.getKey().indexOf("("));
                            String mappingDescription = mappingElement.getKey().substring(mappingElement.getKey().lastIndexOf("("));
                            String mappingObf = SMA.INSTANCE.methodCleanToObfMap.get(type + "/" + mappingName + " " + mappingDescription);
                            mappingObf = "L" + mappingObf.split(" ")[0].replace("/", ";") + mappingObf.split(" ")[1];
                            newMapping.addProperty(mappingElement.getKey(), mappingObf);
                        }
                    }
                    newJsonMappings.add(mappingsElement.getKey(), newMapping);
                }
                newJsonObject.add("mappings", newJsonMappings);

                FileUtils.write(refmapFile + "_update." + refmapSuffix, new GsonBuilder().setPrettyPrinting().create().toJson(newJsonObject.getAsJsonObject()));
            }
            case 5: {
                System.out.println("Please use annotation processor\nOnly support @Mixin and @Inject");
                break;
            }
        }
    }

    private static String internalize(String name) {
        switch (name) {
            case "int":
                return "I";
            case "float":
                return "F";
            case "double":
                return "D";
            case "long":
                return "J";
            case "boolean":
                return "Z";
            case "short":
                return "S";
            case "byte":
                return "B";
            case "void":
                return "V";
            default:
                return name.replace('.', '/');
        }
    }

    private static boolean isPrimitive(String name) {
        switch (name) {
            case "int":
            case "float":
            case "double":
            case "long":
            case "boolean":
            case "short":
            case "byte":
            case "void":
                return true;
            default:
                return false;
        }
    }

    public static void proguardToSrg(String proguardPath) {
        String proguardFile = proguardPath.substring(0, proguardPath.lastIndexOf("."));
        String proguardSuffix = proguardPath.substring(proguardPath.lastIndexOf(".") + 1);
        String text = Objects.requireNonNull(FileUtils.read(proguardPath));
        {
            String[] lines = text.split("\\r\\n|\\n");
            for (String line : lines) {
                if (line.startsWith("#"))
                    continue;

                //class
                if (line.matches(NAME_LINE)) {
                    String[] split = line.split(SPLITTER);
                    String clean = internalize(split[0]);
                    String obf = internalize(split[1]);
                    obf = obf.substring(0, obf.indexOf(':'));
                    SMA.INSTANCE.classObfToCleanMap.put(obf, clean);
                    SMA.INSTANCE.classCleanToObfMap.put(clean, obf);
                }
            }
        }

        {
            String[] lines = text.split("\\r\\n|\\n");
            String currentObfClass = null;
            String currentCleanClass = null;
            for (String line : lines) {
                if (line.startsWith("#"))
                    continue;

                if (line.matches(NAME_LINE)) {
                    currentObfClass = line.substring(line.lastIndexOf(" ") + 1, line.indexOf(":"));
                    currentCleanClass = SMA.INSTANCE.classObfToCleanMap.getOrDefault(currentObfClass, internalize(currentObfClass));
                    continue;
                }

                if (currentObfClass == null)
                    throw new NullPointerException(line + " No Class");


                if (!line.contains("(")) {
                    //Field
                    String[] split = line.trim().split(SPLITTER);
                    String clean = currentCleanClass + "/" + split[1];
                    String obf = currentObfClass + "/" + split[2];
                    SMA.INSTANCE.fieldObfToCleanMap.put(obf, clean);
                    SMA.INSTANCE.fieldCleanToObfMap.put(clean, obf);
                } else {
                    //Method
                    String[] split = line.contains(":") ? line.substring(line.lastIndexOf(":") + 1).trim().split(SPLITTER) : line.trim().split(SPLITTER);
                    String cleanReturn = !isPrimitive(split[0]) ? "L" + internalize(split[0]) + ";" : internalize(split[0]);
                    String cleanName = split[1].substring(0, split[1].lastIndexOf("("));
                    String cleanArgs = split[1].substring(split[1].indexOf("(") + 1, split[1].lastIndexOf(")"));
                    String obfReturn = !isPrimitive(split[0]) ? "L" + SMA.INSTANCE.classCleanToObfMap.getOrDefault(internalize(split[0]), internalize(split[0])) + ";" : cleanReturn;
                    String obfName = split[2];
                    String obfArgs;

                    if (!cleanArgs.equals("")) {
                        StringBuilder tempCleanArs = new StringBuilder();
                        StringBuilder tempObfArs = new StringBuilder();
                        for (String s : cleanArgs.split(",")) {
                            if (!isPrimitive(s)) {
                                tempObfArs.append("L").append(SMA.INSTANCE.classCleanToObfMap.getOrDefault(internalize(s), internalize(s))).append(";");
                                tempCleanArs.append("L").append(internalize(s)).append(";");
                            } else {
                                tempObfArs.append(internalize(s));
                                tempCleanArs.append(internalize(s));
                            }
                        }
                        obfArgs = "(" + tempObfArs.toString() + ")";
                        cleanArgs = "(" + tempCleanArs.toString() + ")";
                    } else {
                        obfArgs = "()";
                        cleanArgs = "()";
                    }

                    String obf = currentObfClass + "/" + obfName + " " + obfArgs + obfReturn;
                    String clean = currentCleanClass + "/" + cleanName + " " + cleanArgs + cleanReturn;
                    SMA.INSTANCE.methodObfToCleanMap.put(obf, clean);
                    SMA.INSTANCE.methodCleanToObfMap.put(clean, obf);
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        SMA.INSTANCE.classObfToCleanMap.forEach((k, v) -> stringBuilder.append("CL: ").append(k).append(" ").append(v).append("\n"));
        SMA.INSTANCE.fieldObfToCleanMap.forEach((k, v) -> stringBuilder.append("FD: ").append(k).append(" ").append(v).append("\n"));
        SMA.INSTANCE.methodObfToCleanMap.forEach((k, v) -> stringBuilder.append("MD: ").append(k).append(" ").append(v).append("\n"));
        FileUtils.write(proguardFile + "_to_srg." + proguardSuffix, stringBuilder.toString());
    }

    public static void remapJar(String jarPath, boolean deObf) throws IOException {

        String jarFile = jarPath.substring(0, jarPath.lastIndexOf("."));
        String jarSuffix = jarPath.substring(jarPath.lastIndexOf(".") + 1);

        putRemap(deObf);

        ZipOutputStream jarOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile + (deObf ? "_DeObf." : "_ReObf.") + jarSuffix)), StandardCharsets.UTF_8);
        JarFile jar = new JarFile(new File(jarPath));
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            jarOutputStream.setComment("SrgMappingAnalyze by Enaium");
            if (entry.isDirectory())
                continue;
            if (entry.getName().endsWith(".class")) {
                jarOutputStream.putNextEntry(new JarEntry(map.getOrDefault(entry.getName().replace(".class", ""), entry.getName()) + ".class"));
                acceptClass(jar.getInputStream(entry), jarOutputStream);
            } else {
                jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                copyFile(jar.getInputStream(entry), jarOutputStream);
            }
            jarOutputStream.closeEntry();
        }
        jarOutputStream.close();
    }

    public static void putRemap(boolean deObf) {
        if (deObf) {
            map.putAll(SMA.INSTANCE.classObfToCleanMap);
        } else {
            map.putAll(SMA.INSTANCE.classCleanToObfMap);
        }

        SMA.INSTANCE.fieldObfToCleanMap.forEach((k, v) -> {
            String key = deObf ? k : v;
            String value = deObf ? v : k;
            String obfClassName = key.substring(0, key.lastIndexOf("/"));
            String obfFiledName = key.substring(key.lastIndexOf("/") + 1);
            map.put(obfClassName + "." + obfFiledName, value.substring(value.lastIndexOf("/") + 1));
        });

        SMA.INSTANCE.methodObfToCleanMap.forEach((k, v) -> {
            String key = deObf ? k : v;
            String value = deObf ? v : k;
            String obfLeft = key.split(" ")[0];
            String obfRight = key.split(" ")[1];
            String cleanLeft = value.split(" ")[0];
            String cleanMethodName = cleanLeft.substring(cleanLeft.lastIndexOf("/") + 1);
            String obfClassName = obfLeft.substring(0, obfLeft.lastIndexOf("/"));
            String obfMethodName = obfLeft.substring(obfLeft.lastIndexOf("/") + 1);
            map.put(obfClassName + "." + obfMethodName + obfRight, cleanMethodName);
        });
    }

    public static void acceptClass(InputStream input, OutputStream output) throws IOException {
        output.write(accept(IOUtils.toByteArray(input)));
        output.flush();
    }

    public static byte[] accept(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM5, classWriter) {
        }, new SimpleRemapper(map) {
        });
        classReader.accept(classRemapper, ClassReader.SKIP_DEBUG);
        return classWriter.toByteArray();
    }


    public static void copyFile(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int c;
        while ((c = input.read(buffer)) != -1) {
            output.write(buffer, 0, c);
        }
    }
}
