# SrgMappingAnalyze

Use `java -cp .\SrgMappingAnalyze-1.0.jar cn.enaium.sma.SrgMappingAnalyze`

## Features

### Mapping key to value,value to key

Input Srg Mapping

### Proguard mapping to Srg mapping

Input Proguard Mapping

### Mixin refmap update

Input refmap.json

### Mixin refmap builder

```groovy
repositories {
    maven { url 'https://maven.enaium.cn/' }
}

dependencies {
    annotationProcessor implementation('cn.enaium:SrgMappingAnalyze:0.1')
    implementation('cn.enaium:SrgMappingAnalyze:0.1')
}

compileJava {
    options.compilerArgs << "-Acn.enaium.sma.SrgMapping=srg_client.txt"
    options.compilerArgs << "-Acn.enaium.sma.refmap=mixins.refmap.json"
}
```

Mixin

```java
@ReMap("net.minecraft.client.Minecraft")
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ReMap("run()V")
    @Inject(at = @At("HEAD"), method = "run()V", remap = false)
    public void run(CallbackInfo callbackInfo) {
    }
}
```