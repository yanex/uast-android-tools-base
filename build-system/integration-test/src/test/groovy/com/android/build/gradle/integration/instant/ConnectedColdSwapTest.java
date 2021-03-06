/*
 * Copyright (C) 2016 The Android Open Source Project
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
import static com.android.build.gradle.integration.common.utils.AndroidVersionMatcher.thatUsesArt;
import static com.android.build.gradle.integration.common.utils.AndroidVersionMatcher.thatUsesDalvik;

import com.android.annotations.NonNull;
import com.android.build.gradle.OptionalCompilationStep;
import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.Adb;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.Logcat;
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp;
import com.android.build.gradle.internal.incremental.ColdswapMode;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.InstantRun;
import com.android.ddmlib.IDevice;
import com.android.ide.common.packaging.PackagingUtils;
import com.android.tools.fd.client.AppState;
import com.android.tools.fd.client.InstantRunBuildInfo;
import com.android.tools.fd.client.InstantRunClient;
import com.android.tools.fd.client.UpdateMode;
import com.android.tools.fd.client.UserFeedback;
import com.android.utils.ILogger;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Connected smoke test for cold swap.
 */
@Category(DeviceTests.class)
@RunWith(MockitoJUnitRunner.class)
public class ConnectedColdSwapTest {

    @Rule
    public GradleTestProject project = GradleTestProject.builder()
            .fromTestApp(HelloWorldApp.forPlugin("com.android.application")).create();

    @Rule
    public Adb adb = Adb.create();

    @Rule
    public Expect expect = Expect.create();

    @Rule
    public Logcat logcat = Logcat.create();

    @Rule
    public TestWatcher onFailure = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            InstantRunTestUtils.printBuildInfoFile(instantRunModel);
        }
    };

    @Mock
    UserFeedback userFeedback;

    @Mock
    ILogger iLogger;

    @Before
    public void activityClass() throws IOException {
        createActivityClass("Logger.getLogger(\"coldswaptest\").warning(\"coldswaptest_before\");\n");
    }

    @Test
    public void dalvikTest() throws Exception {
        doTest(ColdswapMode.DEFAULT, adb.getDevice(thatUsesDalvik()));
    }

    @Test
    public void multiApkTest() throws Exception {
        doTest(ColdswapMode.MULTIAPK, adb.getDevice(thatUsesArt()));
    }

    @Test
    public void multidexTest() throws Exception {
        doTest(ColdswapMode.MULTIDEX, adb.getDevice(thatUsesArt()));
    }

    private InstantRun instantRunModel;

    private void doTest(@NonNull ColdswapMode coldswapMode, @NonNull IDevice device)
            throws Exception {
        // Set up
        logcat.start(device, "coldswaptest");
        AndroidProject model = project.getSingleModel();
        instantRunModel = InstantRunTestUtils.getInstantRunModel(model);
        long token = PackagingUtils.computeApplicationHash(model.getBuildFolder());

        // Initial build
        project.execute(InstantRunTestUtils
                        .getInstantRunArgs(device, coldswapMode, OptionalCompilationStep.RESTART_ONLY),
                "clean", "assembleDebug");

        InstantRunBuildInfo info = InstantRunTestUtils.loadContext(instantRunModel);
        InstantRunTestUtils.doInstall(device, info.getArtifacts());
        InstantRunTestUtils.unlockDevice(device);
        Logcat.MessageListener messageListener = logcat.listenForMessage("coldswaptest_before");
        InstantRunTestUtils.runApp(device, "com.example.helloworld/.HelloWorld");

        //Connect to device
        InstantRunClient client =
                new InstantRunClient("com.example.helloworld", userFeedback, iLogger, token, 8125);

        // Give the app a chance to start
        messageListener.await();

        // Check the app is running
        assertThat(client.getAppState(device)).isEqualTo(AppState.FOREGROUND);

        // Cold swap
        makeColdSwapChange();
        project.execute(InstantRunTestUtils.getInstantRunArgs(device, coldswapMode),
                instantRunModel.getIncrementalAssembleTaskName());

        InstantRunBuildInfo coldSwapContext = InstantRunTestUtils.loadContext(instantRunModel);

        if (thatUsesDalvik().matches(device.getVersion())) {
            // No artifact should have been produced, and the verifier is marked as failed,
            // so studio knows to then call assembleDebug.
            assertThat(coldSwapContext.canHotswap()).named("verifier passed").isFalse();
            assertThat(coldSwapContext.getArtifacts()).isEmpty();
            device.uninstallPackage("com.example.helloworld");
            return;
        }

        if (coldswapMode == ColdswapMode.MULTIAPK) {
            InstantRunTestUtils.doInstall(device, info.getArtifacts());
        } else {
            UpdateMode updateMode = client
                    .pushPatches(device, coldSwapContext,
                            UpdateMode.HOT_SWAP,
                            /* NB: Intentionally HOT_SWAP, pushPatches should automatically
                               determine that the changes cannot be hot-swapped */
                            false /*restartActivity*/,
                            true /*showToast*/);

            assertThat(updateMode).named("updateMode").isEqualTo(UpdateMode.COLD_SWAP);
            Mockito.verify(userFeedback).notifyEnd(UpdateMode.COLD_SWAP);
        }
        Mockito.verifyNoMoreInteractions(userFeedback);

        Logcat.MessageListener afterMessageListener = logcat.listenForMessage("coldswaptest_after");

        InstantRunTestUtils.runApp(device, "com.example.helloworld/.HelloWorld");

        // Check the app is running
        afterMessageListener.await();
        assertThat(client.getAppState(device)).isEqualTo(AppState.FOREGROUND);

        device.uninstallPackage("com.example.helloworld");
    }

    private void makeColdSwapChange() throws IOException {
        createActivityClass("newMethod();\n"
                + "    }\n"
                + "    public void newMethod() {\n"
                + "        Logger.getLogger(\"coldswaptest\").warning(\"coldswaptest_after\");\n"
                + "");
    }

    private void createActivityClass(@NonNull String newMethodBody)
            throws IOException {
        String javaCompile = "package com.example.helloworld;\n"
                + "\n"
                + "import java.util.logging.Logger;\n" +
                "\n"
                + "import android.app.Activity;\n"
                + "import android.os.Bundle;\n"
                + "\n"
                + "public class HelloWorld extends Activity {\n"
                + "    /** Called when the activity is first created. */\n"
                + "    @Override\n"
                + "    public void onCreate(Bundle savedInstanceState) {\n"
                + "        super.onCreate(savedInstanceState);\n"
                + "        setContentView(R.layout.main);\n"
                + "        " +
                newMethodBody +
                "    }\n"
                + "}";
        Files.write(javaCompile,
                project.file("src/main/java/com/example/helloworld/HelloWorld.java"),
                Charsets.UTF_8);
    }

}
