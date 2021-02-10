package com.linkedin.gradle;

import com.android.build.api.variant.BuiltArtifact;
import com.android.build.api.variant.BuiltArtifacts;
import com.android.build.api.variant.BuiltArtifactsLoader;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class FindApkTask extends DefaultTask {

    @InputFiles
    public abstract DirectoryProperty getApkFolder();

    @Internal
    public abstract Property<BuiltArtifactsLoader> getBuiltArtifactsLoader();

    @OutputFile
    public abstract RegularFileProperty getOutputApk();

    @TaskAction
    void findDebugApk() throws IOException {
        BuiltArtifacts builtArtifacts = getBuiltArtifactsLoader().get().load(getApkFolder().get());
        int size = builtArtifacts.getElements().size();
        if (size != 1) {
            throw new GradleException("BuiltArtifacts was not exactly 1 (was " + size + ")");
        }
        Path outputPath = getOutputApk().get().getAsFile().toPath();
        Files.deleteIfExists(outputPath);
        for (BuiltArtifact element : builtArtifacts.getElements()) {
            Files.copy(Paths.get(element.getOutputFile()), outputPath);
        }
    }
}
