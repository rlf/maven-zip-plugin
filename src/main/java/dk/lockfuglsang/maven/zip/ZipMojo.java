package dk.lockfuglsang.maven.zip;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple mojo for zipping multiple file-entries (ant-foreach of zip, or simplified assembly)
 */
@Mojo(name = "zip")
public class ZipMojo extends AbstractMojo {

    @Parameter(property = "zip.srcDir", required = true)
    private File srcDir;

    @Parameter(property = "zip.tgtDir", defaultValue = "${project.build.directory}")
    private File tgtDir;

    @Parameter(property = "zip.extension", defaultValue = ".zip")
    private String extension;

    @Parameter(property = "zip.includes")
    private List<String> includes;

    private Path srcPath;
    private Path tgtPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (srcDir != null && tgtDir != null && srcDir.exists()) {
            tgtDir.mkdirs();
            srcPath = Paths.get(srcDir.toURI());
            tgtPath = Paths.get(tgtDir.toURI());
            try {
                Files.walkFileTree(srcPath, new ZipVisitor());
            } catch (IOException e) {
                throw new MojoFailureException("Unable to scan srcDir", e);
            }
        }
    }

    private class ZipVisitor implements FileVisitor<Path> {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relative = srcPath.relativize(file);
            if (isIncluded(relative)) {
                makeZip(relative);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }

    private void makeZip(Path relative) throws IOException {
        Path targetFile = tgtPath.resolve(relative);
        Path parentDir = targetFile.getParent();
        String filename = targetFile.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.')) + extension;
        Path tgtFile = parentDir.resolve(filename);
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tgtFile.toFile()))) {
            byte[] bytes = Files.readAllBytes(srcPath.resolve(relative));
            ZipEntry zipEntry = new ZipEntry(relative.getFileName().toString());
            zout.putNextEntry(zipEntry);
            zout.write(bytes);
            zout.closeEntry();
        }
    }

    private boolean isIncluded(Path relative) {
        if (includes == null || includes.isEmpty()) {
            return true;
        }
        for (String pattern : includes) {
            if (relative.toString().matches(pattern.replaceAll("\\*\\*", "*").replaceAll("\\*", ".*"))) {
                return true;
            }
        }
        return false;
    }
}
