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

package com.android.build.gradle.internal;

import org.gradle.language.base.internal.registry.LanguageRegistry;
import org.gradle.language.base.internal.registry.LanguageRegistration;

/**
 * Utitities for handling {@link LanguageRegistry}.
 */
public class LanguageRegistryUtils {
    public static LanguageRegistration find(LanguageRegistry languageRegistry, Class sourceSetClass) {
        for (LanguageRegistration languageRegistration : languageRegistry) {
            if (languageRegistration.getSourceSetType().equals(sourceSetClass)) {
                return languageRegistration;
            }
        }
        // should not happen
        throw new RuntimeException(
                "Failed to find language registration for " + sourceSetClass.getName());
    }


}
