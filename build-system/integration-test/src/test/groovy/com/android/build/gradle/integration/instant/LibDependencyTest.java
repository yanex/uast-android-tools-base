/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.integration.instant;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;
import static com.android.build.gradle.integration.common.utils.TestFileUtils.appendToFile;
import static com.android.build.gradle.integration.instant.InstantRunTestUtils.getInstantRunModel;
import static com.android.utils.FileUtils.mkdirs;

import com.android.annotations.NonNull;
import com.android.build.gradle.OptionalCompilationStep;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.truth.AbstractAndroidSubject;
import com.android.build.gradle.integration.common.truth.ApkSubject;
import com.android.build.gradle.integration.common.truth.DexFileSubject;
import com.android.build.gradle.integration.common.truth.FileSubject;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.internal.incremental.ColdswapMode;
import com.android.build.gradle.internal.incremental.InstantRunVerifierStatus;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.InstantRun;
import com.android.builder.model.Variant;
import com.android.ide.common.process.ProcessException;
import com.android.tools.fd.client.InstantRunArtifact;
import com.android.tools.fd.client.InstantRunArtifactType;
import com.android.tools.fd.client.InstantRunBuildInfo;
import com.android.utils.FileUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.truth.Expect;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hot swap test for sub projects.
 */
public class LibDependencyTest {

    @Rule
    public GradleTestProject project =
            GradleTestProject.builder()
                .fromTestProject("libDependency")
                .create();

    @Rule
    public Expect expect = Expect.create();

    @Before
    public void activityClass() throws IOException {
        createLibraryClass("Before");
    }

    @Test
    public void buildIncrementallyWithInstantRun() throws IOException, ProcessException {
        project.execute("clean");
        Map<String, AndroidProject> projects = project.getAllModels();
        InstantRun instantRunModel = getInstantRunModel(projects.get(":app"));

        // Check that original class is included.
        project.execute(getInstantRunArgs(), "clean", "assembleRelease", "assembleDebug");
        expect.about(ApkSubject.FACTORY)
                .that(project.getSubproject("app").getApk("debug"))
                .getClass("Lcom/android/tests/libstest/lib/MainActivity;",
                        AbstractAndroidSubject.ClassFileScope.MAIN)
                .that().hasMethod("onCreate");

        checkHotSwapCompatibleChange(instantRunModel);
    }

    @Test
    public void hotSwapChangeInJavaLibrary() throws Exception {
        appendToFile(project.file("settings.gradle"), "\n"
                + "include 'javalib'\n");
        appendToFile(project.file("lib/build.gradle"), "\n"
                + "dependencies {\n"
                + "    compile project(':javalib')\n"
                + "}\n");
        mkdirs(project.file("javalib"));
        Files.write("apply plugin: 'java'\n", project.file("javalib/build.gradle"), Charsets.UTF_8);
        createJavaLibraryClass("original");

        Map<String, AndroidProject> projects = project.getAllModels();
        InstantRun instantRunModel = getInstantRunModel(projects.get(":app"));
        project.execute(
                InstantRunTestUtils.getInstantRunArgs(
                        23, ColdswapMode.MULTIDEX, OptionalCompilationStep.RESTART_ONLY),
                "clean", ":app:assembleDebug");
        createJavaLibraryClass("changed");
        project.execute(InstantRunTestUtils.getInstantRunArgs(23, ColdswapMode.MULTIDEX),
                instantRunModel.getIncrementalAssembleTaskName());
        InstantRunBuildInfo context = InstantRunTestUtils.loadContext(instantRunModel);
        assertThat(context.getVerifierStatus()).isEqualTo(
                InstantRunVerifierStatus.COMPATIBLE.toString());
        assertThat(context.getArtifacts()).hasSize(1);
        InstantRunArtifact artifact = Iterables.getOnlyElement(context.getArtifacts());
        assertThat(artifact.type).isEqualTo(InstantRunArtifactType.RELOAD_DEX);
    }

    @Test
    public void checkVerifierFailsIfJavaResourceInLibraryChanged() throws Exception {
        File resource = project.getSubproject(":lib").file("src/main/resources/properties.txt");
        Files.write("java resource", resource, Charsets.UTF_8);

        project.execute("clean");
        Map<String, AndroidProject> projects = project.getAllModels();
        InstantRun instantRunModel = getInstantRunModel(projects.get(":app"));

        // Check that original class is included.
        project.execute(getInstantRunArgs(), "clean", "assembleDebug");
        expect.about(ApkSubject.FACTORY)
                .that(project.getSubproject("app").getApk("debug"))
                .getClass("Lcom/android/tests/libstest/lib/MainActivity;",
                        AbstractAndroidSubject.ClassFileScope.MAIN)
                .that().hasMethod("onCreate");

        Files.write("changed java resource", resource, Charsets.UTF_8);

        project.execute(getInstantRunArgs(), instantRunModel.getIncrementalAssembleTaskName());
        InstantRunBuildInfo context = InstantRunTestUtils.loadContext(instantRunModel);
        assertThat(context.getVerifierStatus()).isEqualTo(
                InstantRunVerifierStatus.JAVA_RESOURCES_CHANGED.toString());
        assertThat(context.getArtifacts()).hasSize(0);
    }

    /**
     * Check a hot-swap compatible change works as expected.
     */
    private void checkHotSwapCompatibleChange(@NonNull InstantRun instantRunModel)
            throws IOException, ProcessException {
        createLibraryClass("Hot swap change");

        project.execute(getInstantRunArgs(), instantRunModel.getIncrementalAssembleTaskName());

        // TODO: The instant run model methods are gone -- this test needs to be
        // updated to use
        //    instantRunModel.getInfoFile()
        // (See InstantRunBuildInfo in the IDE)
        //expect.about(DexFileSubject.FACTORY)
        //        .that(instantRunModel.getReloadDexFile())
        //        .hasClass("Lcom/android/tests/libstest/lib/Lib$override;");
        //
        //// the restart .dex should not be present.
        //expect.about(FileSubject.FACTORY).that(instantRunModel.getRestartDexFile()).doesNotExist();
    }

    private void createLibraryClass(String message)
            throws IOException {
        String javaCompile = "package com.android.tests.libstest.lib;\n"
            +"public class Lib {\n"
                +"public static String someString() {\n"
                  +"return \"someStringMessage=" + message + "\";\n"
                +"}\n"
            +"}\n";
        Files.write(javaCompile,
                project.file("lib/src/main/java/com/android/tests/libstest/lib/Lib.java"),
                Charsets.UTF_8);
    }

    private void createJavaLibraryClass(String message)
            throws IOException {
        File dir = project.file("javalib/src/main/java/com/example/javalib");
        mkdirs(dir);
        String java = "package com.example.javalib;\n"
                +"public class A {\n"
                +"    public static String someString() {\n"
                +"        return \"someStringMessage=" + message + "\";\n"
                +"    }\n"
                +"}\n";
        Files.write(java, new File(dir, "A.java"), Charsets.UTF_8);
    }

    private static List<String> getInstantRunArgs(OptionalCompilationStep... flags) {
        String property = "-P" + AndroidProject.OPTIONAL_COMPILATION_STEPS + "=" +
                OptionalCompilationStep.INSTANT_DEV + "," + Joiner.on(',').join(flags);
        return Collections.singletonList(property);
    }
}
