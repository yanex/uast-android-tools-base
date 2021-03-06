/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.integration.dependencies;

import static com.android.build.gradle.integration.common.utils.TestFileUtils.appendToFile;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.truth.TruthHelper;
import com.android.build.gradle.integration.common.utils.ModelHelper;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.ide.common.process.ProcessException;
import com.google.common.truth.Truth;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * test for package (apk) jar in app
 */
public class AppWithPackageDirectJarTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("projectWithModules")
            .create();
    static Map<String, AndroidProject> models;

    @BeforeClass
    public static void setUp() throws IOException {
        appendToFile(project.getSubproject("app").getBuildFile(),
                "\n" +
                "dependencies {\n" +
                "    apk project(\":jar\")\n" +
                "}\n");
        models = project.executeAndReturnMultiModel("clean", ":app:assembleDebug");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
        models = null;
    }

    @Test
    public void checkPackageJarIsPackaged() throws IOException, ProcessException {
        assertThatApk(project.getSubproject("app").getApk("debug"))
                .containsClass("Lcom/example/android/multiproject/person/People;");
    }

    @Test
    public void checkPackagedJarIsNotInTheModel() {
        Variant variant = ModelHelper.getVariant(models.get(":app").getVariants(), "debug");
        Truth.assertThat(variant).isNotNull();

        Dependencies deps = variant.getMainArtifact().getDependencies();
        TruthHelper.assertThat(deps.getProjects()).named("Project deps").isEmpty();
    }

    @Test
    public void checkPackageJarIsInTheAndroidTestDeps() {
        // TODO
    }

    @Test
    public void checkPackageJarIsIntheUnitTestDeps() {
        // TODO
    }
}
