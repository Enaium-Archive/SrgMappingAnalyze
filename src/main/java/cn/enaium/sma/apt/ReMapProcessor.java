package cn.enaium.sma.apt;

import cn.enaium.sma.SMA;
import cn.enaium.sma.apt.annotation.ReMap;
import cn.enaium.sma.exception.NoArgumentException;
import cn.enaium.sma.utils.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Project: SrgMappingAnalyze
 * Author: Enaium
 */
@SupportedOptions({ReMapProcessor.srgMappingName, ReMapProcessor.refmapName})
public class ReMapProcessor extends AbstractProcessor {

    public static final String srgMappingName = "cn.enaium.sma.SrgMapping";
    public static final String refmapName = "cn.enaium.sma.refmap";
    private String refmap;
    private JsonObject jsonObject = new JsonObject();


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        String srgFile = processingEnv.getOptions().get(srgMappingName);
        if (srgFile == null) {
            throw new NoArgumentException(srgMappingName);
        }
        SMA.INSTANCE.parseMapping(Objects.requireNonNull(FileUtils.read(srgFile)));

        refmap = processingEnv.getOptions().get(refmapName);
        if (refmap == null) {
            throw new NoArgumentException(refmapName);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ReMap.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            JsonObject mappingObject = new JsonObject();
            for (Element element : roundEnv.getElementsAnnotatedWith(ReMap.class)) {
                if (!element.getKind().isClass() && !element.getKind().isInterface()) {
                    String mixin = element.getEnclosingElement().toString().replace(".", "/");
                    String classClean = element.getEnclosingElement().getAnnotation(ReMap.class).value().replace(".", "/");
                    String remap = element.getAnnotation(ReMap.class).value();
                    if (remap.contains(" ")) {
                        throw new NullPointerException(mixin + " " + remap + " Empty characters");
                    }
                    JsonObject mapping = new JsonObject();
                    if (element.getKind().equals(ElementKind.METHOD) && !element.getEnclosingElement().getKind().isInterface()) {
                        if (!remap.contains("(") || !remap.contains(")") || remap.split("\\)").length == 1) {
                            throw new NullPointerException(mixin + " " + remap + " No Description");
                        }

                        String mappingName = remap.substring(0, remap.indexOf("("));
                        String mappingDescription = remap.substring(remap.lastIndexOf("("));
                        String mappingObf = SMA.INSTANCE.methodCleanToObfMap.get(classClean + "/" + mappingName + " " + mappingDescription);
                        if (mappingObf == null) {
                            throw new NullPointerException(mixin + " " + remap + " No mapping");
                        }
                        mappingObf = "L" + mappingObf.split(" ")[0].replace("/", ";") + mappingObf.split(" ")[1];
                        mapping.addProperty(remap, mappingObf);
                    }

                    if (!mappingObject.has(mixin)) {
                        mappingObject.add(mixin, mapping);
                    } else {
                        //Once loop
                        mapping.entrySet().forEach(entry -> mappingObject.get(mixin).getAsJsonObject().add(entry.getKey(), entry.getValue()));
                    }
                }
            }
            jsonObject.add("mappings", mappingObject);
            genFileRefmap();

        }
        return false;
    }

    private void genFileRefmap() {
        Filer filer = processingEnv.getFiler();
        try {
            FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", refmap);
            OutputStream out = fileObject.openOutputStream();
            out.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject.getAsJsonObject()).toString().getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (IOException e) {
            throw new NullPointerException(refmap);
        }
    }
}
