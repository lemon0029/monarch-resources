package me.monarch;

import me.monarch.tools.ClassFile;
import me.monarch.tools.ClassFileConverter;
import me.monarch.tools.ScFile;
import me.monarch.tools.ScFileLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main {

    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        process(userDir + "/package-files/input/1.63.0109.zip");
        process(userDir + "/package-files/input/1.64.0518.zip");
        process(userDir + "/package-files/input/com.gamebox.kingSanguoAppstore-1.62.0428.ipa");
        process(userDir + "/package-files/input/com.gamebox.kingSanguoAppstore-1.61.1206.ipa");
        process(userDir + "/package-files/input/com.gameme5.www_1.60.0617_und3fined.ipa");
    }

    private static void process(String file) throws IOException {
        Path path = Paths.get(file);
        List<ScFile> scFiles = ScFileLoader.load(path);

        String packageVersion = scFiles.stream()
                .findAny()
                .map(it -> it.packageVersion)
                .orElseThrow();

        List<ClassFile> classFiles = ClassFileConverter.convert(scFiles);

        collectToJar(packageVersion, classFiles);
    }

    private static void collectToJar(String packageVersion, List<ClassFile> classFiles) throws IOException {
        String userDir = System.getProperty("user.dir");
        Path outputFile = Path.of(userDir, "package-files", "output", "dwsg-client-%s.jar".formatted(packageVersion));

        if (Files.exists(outputFile)) {
            return;
        }

        Path outputDir = outputFile.getParent();
        if (Files.notExists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        try (OutputStream outputStream = Files.newOutputStream(outputFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
            zipOutputStream.putNextEntry(manifestEntry);
            zipOutputStream.write("Manifest-Version 1.0\n\n".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            for (ClassFile classFile : classFiles) {
                ZipEntry zipEntry = new ZipEntry(classFile.fullName.replace(".", "/") + ".class");
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(classFile.data);
                zipOutputStream.closeEntry();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
