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

package com.android.tools.lint.client.api;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.google.common.base.Joiner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;

import java.util.ArrayList;
import java.util.List;

public class UastLintUtils {
    public static class AndroidReference {
        private final String rPackage;
        private final ResourceType type;
        private final String name;

        // getPackage() can be empty if not a package-qualified import (e.g. android.R.id.name).
        @NonNull
        public String getPackage() {
            return rPackage;
        }

        @NonNull
        public ResourceType getType() {
            return type;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public AndroidReference(String rPackage, ResourceType type, String name) {
            this.rPackage = rPackage;
            this.type = type;
            this.name = name;
        }
    }

    public static List<UAnnotation> getAllAnnotations(UFunction function, UastContext context) {
        List<UAnnotation> annotations = new ArrayList<UAnnotation>(function.getAnnotations());
        for (UFunction superFunction : function.getSuperFunctions(context)) {
            annotations.addAll(superFunction.getAnnotations());
        }
        return annotations;
    }

    public static List<UAnnotation> getAllAnnotationsInHierarchy(UAnnotated annotated) {
        List<UAnnotation> annotations = new ArrayList<UAnnotation>(0);
        UElement element = annotated;
        while (element != null) {
            if (element instanceof UAnnotated) {
                annotations.addAll(((UAnnotated) element).getAnnotations());
            }
            element = element.getParent();
        }
        return annotations;
    }

    public static AndroidReference toAndroidReference(
            UQualifiedExpression expression,
            @Nullable UastContext context) {
        List<String> path = UastUtils.asQualifiedPath(expression);

        String containingClassFqName =
                UastUtils.getContainingClassOrEmpty(expression.resolve(context)).getFqName();

        String packageNameFromResolved = null;
        if (containingClassFqName != null) {
            int i = containingClassFqName.lastIndexOf(".R.");
            if (i >= 0) {
                packageNameFromResolved = containingClassFqName.substring(0, i);
            }
        }

        if (path == null) {
            return null;
        }

        int size = path.size();
        if (size < 3) {
            return null;
        }

        String r = path.get(size - 3);
        if (!r.equals(SdkConstants.R_CLASS)) {
            return null;
        }

        String rPackage = packageNameFromResolved != null
                ? packageNameFromResolved
                : Joiner.on('.').join(path.subList(0, size - 3));

        String type = path.get(size - 2);
        String name = path.get(size - 1);

        ResourceType resourceType = null;
        for (ResourceType value : ResourceType.values()) {
            if (value.getName().equals(type)) {
                resourceType = value;
                break;
            }
        }

        if (resourceType == null) {
            return null;
        }

        return new AndroidReference(rPackage, resourceType, name);
    }

    public static boolean matchesContainingClassFqName(UElement element, String fqName) {
        return UastUtils.getContainingClassOrEmpty(element).matchesFqName(fqName);
    }

    public static boolean isChildOfExpression(
            @NonNull UExpression child,
            @NonNull UExpression parent) {
        UElement current = child;
        while (current != null) {
            if (current.equals(parent)) {
                return true;
            } else if (!(current instanceof UExpression)) {
                return false;
            }

            current = current.getParent();
        }

        return false;
    }

    public static boolean areIdentifiersEqual(UExpression first, UExpression second) {
        String firstIdentifier = getIdentifier(first);
        String secondIdentifier = getIdentifier(second);
        return firstIdentifier != null && secondIdentifier != null
                && firstIdentifier.equals(secondIdentifier);
    }

    @Nullable
    public static String getIdentifier(UExpression expression) {
        if (expression instanceof ULiteralExpression) {
            expression.renderString();
        } else if (expression instanceof USimpleReferenceExpression) {
            return ((USimpleReferenceExpression) expression).getIdentifier();
        } else if (expression instanceof UQualifiedExpression) {
            UQualifiedExpression qualified = (UQualifiedExpression) expression;
            String receiverIdentifier = getIdentifier(qualified.getReceiver());
            String selectorIdentifier = getIdentifier(qualified.getSelector());
            if (receiverIdentifier == null || selectorIdentifier == null) {
                return null;
            }
            return receiverIdentifier + "." + selectorIdentifier;
        }

        return null;
    }

    public static UExpression getQualifiedCallExpression(@NonNull UCallExpression call) {
        UExpression prev = call;
        UElement current = prev.getParent();

        while (current instanceof UQualifiedExpression) {
            UQualifiedExpression qualifiedExpression = (UQualifiedExpression) current;
            if (!qualifiedExpression.getSelector().equals(prev)) {
                return prev;
            }

            prev = qualifiedExpression;
            current = prev.getParent();
        }

        return prev;
    }

    @NotNull
    public static UExpression resolveReferenceInitializer(
            @NotNull UExpression reference, @NotNull UastContext context) {
        UElement current = reference;
        UExpression ret = reference;

        while (current != null) {
            if (current instanceof UExpression) {
                ret = ((UExpression) current);
            }

            if (current instanceof UResolvable) {
                current = ((UResolvable) current).resolve(context);
            } else if (current instanceof UVariable) {
                current = ((UVariable) current).getInitializer();
            } else {
                break;
            }
        }

        return ret;
    }

    public static boolean functionMatches(
            @NonNull UFunction function, @NonNull String... argumentTypes) {
        return functionMatches(function, null, false, argumentTypes);
    }

    public static boolean functionMatches(
            @NonNull UFunction function,
            @Nullable String containingClassFqName,
            boolean allowInherit,
            @NonNull String... argumentTypes) {
        if (containingClassFqName != null && allowInherit) {
            UClass containingClass = UastUtils.getContainingClass(function);
            if (containingClass == null) {
                return false;
            }

            if (!containingClass.isSubclassOf(containingClassFqName, false)) {
                return false;
            }
        }


        return parametersMatch(function, argumentTypes);
    }

    public static boolean parametersMatch(
            @NonNull UFunction function,
            @NonNull String... parameterTypes) {
        if (parameterTypes.length != function.getValueParameterCount()) {
            return false;
        }

        List<UVariable> parameters = function.getValueParameters();
        for (int i = 0; i < parameters.size(); i++) {
            UType type = parameters.get(i).getType();
            String requiredType = parameterTypes[i];
            if (requiredType.equals("java.lang.String") && !type.isString()) {
                return false;
            } else if (!type.matchesFqName(requiredType)) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    public static UElement getParentOfAnyType(
            UElement node,
            boolean strict,
            Class<? extends UElement>... parents) {
        UElement parent = strict ? node.getParent() : node;
        while (parent != null) {
            for (Class<? extends UElement> clazz : parents) {
                if (clazz.isInstance(parent)) {
                    return parent;
                }
            }

            parent = parent.getParent();
        }

        return null;
    }
}
