package me.monarch.tools;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ScFileLoader {

    private static final Pattern REGEX_OF_PACKAGE_VERSION = Pattern.compile("^version.fullnum=(.*?)$");

    public static List<ScFile> load(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return loadFromDirectory(path);
        }

        return loadFromFile(path);
    }

    private static List<ScFile> loadFromFile(Path path) throws IOException {
        FileSystem fileSystem = FileSystems.newFileSystem(path);
        Path payload = fileSystem.getPath("Payload");

        if (Files.exists(payload)) {
            BiPredicate<Path, BasicFileAttributes> predicate = (pt, bfa) -> pt.toString().endsWith(".app");
            Stream<Path> stream = Files.find(payload, 1, predicate);

            try {
                Path app = stream.findFirst().orElseThrow();
                return loadFromDirectory(app);
            } finally {
                stream.close();
                fileSystem.close();
            }
        }

        return loadFromDirectory(fileSystem.getPath("/"));
    }

    private static List<ScFile> loadFromDirectory(Path root) throws IOException {
        Path configFile = root.resolve("script/defautConfig.properties");
        String packageVersion = getPackageVersion(configFile);

        if (packageVersion == null) {
            throw new IllegalStateException("Package version not found");
        }

        Path scriptPagesPath = root.resolve("scriptPages");

        if (!Files.exists(scriptPagesPath)) {
            throw new IllegalStateException("Script pages directory not found");
        }

        List<ScFile> scFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(scriptPagesPath)) {
            List<Path> files = stream.toList();

            for (Path path : files) {
                if (Files.isDirectory(path)) {
                    continue;
                }

                boolean isScFile = path.toString().endsWith(".sc");

                if (!isScFile) {
                    continue;
                }

                ScFile scFile = new ScFile();
                int idx = path.toString().indexOf("scriptPages/");
                scFile.packageVersion = packageVersion;
                scFile.fullName = path.toString().substring(idx + 12);
                scFile.simpleName = path.getFileName().toString();
                scFile.content = Files.readAllBytes(path);

                scFiles.add(scFile);
            }
        }

        return scFiles;
    }

    private static String getPackageVersion(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            Matcher matcher = REGEX_OF_PACKAGE_VERSION.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
