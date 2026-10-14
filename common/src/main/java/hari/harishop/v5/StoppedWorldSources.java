package hari.harishop.v5;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Java-only source discovery for the stopped-world census. */
final class StoppedWorldSources {
    private StoppedWorldSources() {}

    static List<Path> discover(Path root, List<String> failures) {
        List<Path> files = new ArrayList<>();
        collectOptional(root.resolve("playerdata"), files, failures);
        collectOptional(root.resolve("region"), files, failures);
        collectOptional(root.resolve("entities"), files, failures);
        collectOptional(root.resolve("data"), files, failures);
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("DIM"))
                    .forEach(path -> {
                        collectOptional(path.resolve("region"), files, failures);
                        collectOptional(path.resolve("entities"), files, failures);
                        collectOptional(path.resolve("data"), files, failures);
                    });
        } catch (IOException failure) {
            failures.add("dimension-list:" + failure.getClass().getSimpleName());
        }
        // Modern custom dimensions are nested under dimensions/<namespace>/<path>.
        collectOptional(root.resolve("dimensions"), files, failures);
        return files.stream().distinct().sorted(Comparator.comparing(Path::toString)).toList();
    }

    private static void collect(Path directory, List<Path> files, List<String> failures) {
        if (!Files.isDirectory(directory)) return;
        try (var stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".dat") || name.endsWith(".mca");
            }).forEach(files::add);
        } catch (IOException failure) {
            failures.add("walk:" + directory + ':' + failure.getClass().getSimpleName());
        }
    }

    private static void collectOptional(Path directory, List<Path> files, List<String> failures) {
        if (Files.notExists(directory)) return;
        if (!Files.isDirectory(directory)) {
            failures.add("source-not-directory:" + directory.getFileName());
            return;
        }
        collect(directory, files, failures);
    }
}


