package net.minecraftforge.fml.loading.moddiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipError;

import static net.minecraftforge.fml.loading.LogMarkers.SCAN;

public class AbstractJarFileLocator {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final Map<ModFile, FileSystem> modJars;

    public AbstractJarFileLocator() {
        this.modJars = new HashMap<>();
    }

    protected FileSystem createFileSystem(ModFile modFile) {
        try {
            return FileSystems.newFileSystem(modFile.getFilePath(), modFile.getClass().getClassLoader());
        } catch (ZipError | IOException e) {
            LOGGER.debug(SCAN,"Ignoring invalid JAR file {}", modFile.getFilePath());
            return null;
        }
    }

    public Path findPath(final ModFile modFile, final String... path) {
        if (path.length < 1) {
            throw new IllegalArgumentException("Missing path");
        }
        return modJars.get(modFile).getPath(path[0], Arrays.copyOfRange(path, 1, path.length));
    }

    public void scanFile(final ModFile file, final Consumer<Path> pathConsumer) {
        LOGGER.debug(SCAN,"Scan started: {}", file);
        FileSystem fs = modJars.get(file);
        fs.getRootDirectories().forEach(path -> {
            try (Stream<Path> files = Files.find(path, Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
                files.forEach(pathConsumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        LOGGER.debug(SCAN,"Scan finished: {}", file);
    }

    public Optional<Manifest> findManifest(final Path file)
    {
        try (JarFile jf = new JarFile(file.toFile()))
        {
            return Optional.ofNullable(jf.getManifest());
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }
}