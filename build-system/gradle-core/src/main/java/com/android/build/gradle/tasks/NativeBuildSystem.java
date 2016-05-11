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

package com.android.build.gradle.tasks;

import com.android.annotations.NonNull;

/**
 * Enumeration and descriptive metadata for the different external native build system types.
 */
public enum NativeBuildSystem {
    UNKNOWN("unknown"),
    GRADLE("gradle"),
    CMAKE("cmake"),
    NDK_BUILD("ndk-build");

    private final String name;

    NativeBuildSystem(String name) {
        this.name = name;
    }

    /**
     * Returns name of the build system.
     */
    @NonNull
    public String getName() {
        return name;
    }
}