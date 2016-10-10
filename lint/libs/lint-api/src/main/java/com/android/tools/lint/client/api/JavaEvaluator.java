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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.ClassContext;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;

@SuppressWarnings("MethodMayBeStatic") // Some of these methods may be overridden by LintClients
public abstract class JavaEvaluator {
    public static boolean isSubClassOf(
            @Nullable PsiClass cls,
            @NonNull String className,
            boolean strict) {
        return InheritanceUtil.isInheritor(cls, strict, className);
    }

    public static boolean isMemberInSubClassOf(
            @NonNull PsiMember method,
            @NonNull String className,
            boolean strict) {
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && isSubClassOf(containingClass, className, strict);
    }

    public static boolean isMemberInClass(
            @Nullable PsiMember method,
            @NonNull String className) {
        if (method == null) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && className.equals(containingClass.getQualifiedName());
    }

    public static int getParameterCount(@NonNull PsiMethod method) {
        return method.getParameterList().getParametersCount();
    }

    /**
     * Returns true if the given method (which is typically looked up by resolving a method call) is
     * either a method in the exact given class, or if {@code allowInherit} is true, a method in a
     * class possibly extending the given class, and if the parameter types are the exact types
     * specified.
     *
     * @param method        the method in question
     * @param className     the class name the method should be defined in or inherit from (or
     *                      if null, allow any class)
     * @param allowInherit  whether we allow checking for inheritance
     * @param argumentTypes the names of the types of the parameters
     * @return true if this method is defined in the given class and with the given parameters
     */
    public static boolean methodMatches(
            @NonNull PsiMethod method,
            @Nullable String className,
            boolean allowInherit,
            @NonNull String... argumentTypes) {
        if (className != null && allowInherit) {
            if (!isMemberInSubClassOf(method, className, false)) {
                return false;
            }
        }

        return parametersMatch(method, argumentTypes);
    }

    /**
     * Returns true if the given method's parameters are the exact types specified.
     *
     * @param method        the method in question
     * @param argumentTypes the names of the types of the parameters
     * @return true if this method is defined in the given class and with the given parameters
     */
    public static boolean parametersMatch(
            @NonNull PsiMethod method,
            @NonNull String... argumentTypes) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() != argumentTypes.length) {
            return false;
        }
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiType type = parameters[i].getType();
            if (!type.getCanonicalText().equals(argumentTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /** Returns true if the given type matches the given fully qualified type name */
    public static boolean parameterHasType(
            @Nullable PsiMethod method,
            int parameterIndex,
            @NonNull String typeName) {
        if (method == null) {
            return false;
        }
        PsiParameterList parameterList = method.getParameterList();
        return parameterList.getParametersCount() > parameterIndex
                && typeMatches(parameterList.getParameters()[parameterIndex].getType(), typeName);
    }

    /** Returns true if the given type matches the given fully qualified type name */
    public static boolean typeMatches(
            @Nullable PsiType type,
            @NonNull String typeName) {
        return type != null && type.getCanonicalText().equals(typeName);

    }

    public static boolean isPublic(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.PUBLIC);
        }
        return false;
    }

    public static boolean isStatic(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.STATIC);
        }
        return false;
    }

    public static boolean isPrivate(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.PRIVATE);
        }
        return false;
    }

    public static boolean isAbstract(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT);
        }
        return false;
    }

    @Nullable
    public static PsiMethod getSuperMethod(@Nullable PsiMethod method) {
        if (method == null) {
            return null;
        }
        final PsiMethod[] superMethods = method.findSuperMethods();
        if (superMethods.length > 0) {
            return superMethods[0];
        }
        return null;
    }

    @NonNull
    public String getInternalName(@NonNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            qualifiedName = psiClass.getName();
            if (qualifiedName == null) {
                assert psiClass instanceof PsiAnonymousClass;
                //noinspection ConstantConditions
                return getInternalName(psiClass.getContainingClass());
            }
        }
        return ClassContext.getInternalName(qualifiedName);
    }

    @NonNull
    public String getInternalName(@NonNull PsiClassType psiClassType) {
        return ClassContext.getInternalName(psiClassType.getCanonicalText());
    }

    @Nullable
    public PsiClassType getClassType(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }
        return PsiTypesUtil.getClassType(psiClass);
    }

    @NonNull
    public abstract PsiAnnotation[] getAllAnnotations(@NonNull PsiModifierListOwner owner);
}
