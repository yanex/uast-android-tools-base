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

import static org.jetbrains.uast.UastModifier.IMMUTABLE;
import static org.jetbrains.uast.UastModifier.STATIC;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.google.common.base.Joiner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.java.JavaAbstractUExpression;
import org.jetbrains.uast.java.JavaUClass;
import org.jetbrains.uast.java.JavaUFile;
import org.jetbrains.uast.java.JavaUVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UastLintUtils {
    @Nullable
    public static UExpression findLastAssignment(
            @NonNull  UVariable variable,
            @NonNull UElement call,
            @NonNull UastContext context) {
        UElement lastAssignment = null;

        if (!variable.hasModifier(UastModifier.IMMUTABLE) &&
                (variable.getKind() == UastVariableKind.LOCAL_VARIABLE
                        || variable.getKind() == UastVariableKind.VALUE_PARAMETER)) {
            UFunction containingFunction = UastUtils.getContainingFunction(call);
            if (containingFunction != null) {
                ConstantEvaluator.LastAssignmentFinder
                        finder = new ConstantEvaluator.LastAssignmentFinder(
                        variable, call, context, null,
                        (variable.getKind() == UastVariableKind.VALUE_PARAMETER) ? 1 : 0);
                containingFunction.accept(finder);
                lastAssignment = finder.getLastAssignment();
            }
        } else {
            lastAssignment = variable.getInitializer();
        }

        if (lastAssignment instanceof UExpression) {
            return (UExpression) lastAssignment;
        }

        return null;
    }

    @Nullable
    public static Object findLastValue(
            @NonNull  UVariable variable,
            @NonNull UElement call,
            @NonNull ConstantEvaluator evaluator) {
        UastContext context = evaluator.getContext();
        Object value = null;

        if (!variable.hasModifier(UastModifier.IMMUTABLE) &&
                (variable.getKind() == UastVariableKind.LOCAL_VARIABLE
                        || variable.getKind() == UastVariableKind.VALUE_PARAMETER)) {
            UFunction containingFunction = UastUtils.getContainingFunction(call);
            if (containingFunction != null) {
                ConstantEvaluator.LastAssignmentFinder
                        finder = new ConstantEvaluator.LastAssignmentFinder(
                        variable, call, context, evaluator, 1);
                containingFunction.accept(finder);
                value = finder.getCurrentValue();
            }
        } else {
            UExpression initializer = variable.getInitializer();
            if (initializer != null) {
                value = initializer.evaluate();
            }
        }

        return value;
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

    @Nullable
    public static AndroidReference toAndroidReferenceViaResolve(
            UElement element,
            @Nullable UastContext context) {
        if (element instanceof UQualifiedExpression
                && element instanceof JavaAbstractUExpression) {
            AndroidReference ref = toAndroidReference((UQualifiedExpression) element, context);
            if (ref != null) {
                return ref;
            }
        } else if (element instanceof USimpleReferenceExpression
                && element instanceof JavaAbstractUExpression) {
            UExpression maybeQualified = UastUtils.getQualifiedParentOrThis((UExpression) element);
            if (maybeQualified instanceof UQualifiedExpression) {
                AndroidReference ref = toAndroidReference(
                        (UQualifiedExpression) maybeQualified, context);
                if (ref != null) {
                    return ref;
                }
            }
        }

        UDeclaration declaration;
        if (element instanceof UVariable) {
            declaration = (UDeclaration) element;
        } else if (element instanceof UResolvable) {
            declaration = ((UResolvable) element).resolve(context);
        } else {
            return null;
        }

        // Here and below we are using types from the Java Uast implementation by intention
        // (because Android SDK classes are written in Java).

        if (!(declaration instanceof JavaUVariable)) {
            return null;
        }
        UVariable variable = (UVariable) declaration;
        if (variable.getKind() != UastVariableKind.MEMBER || !variable.getType().isInt()) {
            return null;
        }

        if (!variable.hasModifier(STATIC) || !variable.hasModifier(IMMUTABLE)) {
            return null;
        }

        UClass resourceTypeClass = UastUtils.getContainingClass(variable);
        if (!(resourceTypeClass instanceof JavaUClass)) {
            return null;
        }

        if (!resourceTypeClass.hasModifier(UastModifier.STATIC)) {
            return null;
        }

        UClass rClass = UastUtils.getContainingClass(resourceTypeClass);
        if (!(rClass instanceof JavaUClass) || !rClass.matchesName("R")) {
            return null;
        }

        UElement rClassParent = rClass.getParent();
        if (!(rClassParent instanceof JavaUFile)) {
            return null;
        }
        UFile rClassFile = (UFile) rClassParent;
        String packageName = rClassFile.getPackageFqName();

        if (packageName == null || packageName.isEmpty()) {
            return null;
        }

        String resourceTypeName = resourceTypeClass.getName();
        ResourceType resourceType = null;
        for (ResourceType value : ResourceType.values()) {
            if (value.getName().equals(resourceTypeName)) {
                resourceType = value;
                break;
            }
        }

        if (resourceType == null) {
            return null;
        }

        String resourceName = variable.getName();

        UExpression node;
        if (element instanceof UExpression) {
            node = (UExpression) element;
        } else {
            node = new SimpleUDeclarationsExpression(null, Collections.singletonList(element));
        }

        return new AndroidReference(node, packageName, resourceType, resourceName);
    }

    @Nullable
    private static AndroidReference toAndroidReference(
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

        return new AndroidReference(expression, rPackage, resourceType, name);
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
