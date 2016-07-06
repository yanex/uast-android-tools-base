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

package com.android.tools.lint.psiNew;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.EcjParser;
import com.android.tools.lint.ExternalAnnotationRepository;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.ClassContext;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.AtomicNullableLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;

import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewJavaEvaluator extends JavaEvaluator {
    private final PsiManager mManager;

    public NewJavaEvaluator(PsiManager manager) {
        mManager = manager;
    }

    @NonNull
    public static String getTypeName(@NonNull char[][] name) {
        StringBuilder sb = new StringBuilder(50);
        for (char[] segment : name) {
            if (sb.length() != 0) {
                sb.append('.');
            }
            for (char c : segment) {
                if (c == '$') {
                    c = '.';
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @NonNull
    public static String getTypeName(@NonNull char[] name) {
        StringBuilder sb = new StringBuilder(name.length);
        for (char c : name) {
            if (c == '$' || c == '/') {
                c = '.';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @VisibleForTesting
    static boolean equalsCompound(@NonNull String name, @NonNull char[][] compoundName) {
        int length = name.length();
        if (length == 0) {
            return false;
        }
        int index = 0;
        for (int i = 0, n = compoundName.length; i < n; i++) {
            char[] o = compoundName[i];
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0, m = o.length; j < m; j++) {
                if (index == length) {
                    return false; // Don't allow prefix in a compound name
                }
                if (name.charAt(index) != o[j]
                        // Allow using . as an inner class separator whereas the
                        // symbol table will always use $
                        && !(o[j] == '$' && name.charAt(index) == '.')) {
                    return false;
                }
                index++;
            }
            if (i < n - 1) {
                if (index == length) {
                    return false;
                }
                if (name.charAt(index) != '.') {
                    return false;
                }
                index++;
                if (index == length) {
                    return false;
                }
            }
        }

        return index == length;
    }

    @NonNull
    @Override
    public String getInternalName(@NonNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            throw new IllegalArgumentException("qualified name can't be resolved for " + psiClass);
        }
        return ClassContext.getInternalName(qualifiedName);
    }
    
    @NonNull
    @Override
    public String getInternalName(@NonNull PsiClassType psiClassType) {
        PsiClass psiClass = psiClassType.resolve();
        if (psiClass == null) {
            throw new IllegalArgumentException("PsiClass can't be resolved for " + psiClass);
        }
        return getInternalName(psiClass);
    }
    
    @Nullable
    private ExternalAnnotationRepository getAnnotationRepository() {
        AtomicNullableLazyValue<ExternalAnnotationRepository> lazyValue 
                = mManager.getUserData(EcjParser.EXTERNAL_ANNOTATION_REPOSITORY);
        
        if (lazyValue == null) {
            return null;
        }
        
        return lazyValue.getValue();
    }

    @NonNull
    @Override
    public PsiAnnotation[] getAllAnnotations(@NonNull PsiModifierListOwner owner, boolean inHierarchy) {
        if (!inHierarchy) {
            return getDirectAnnotations(owner);
        }

        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return getDirectAnnotations(owner);
        }

        if (owner instanceof PsiMethod) {
            List<PsiAnnotation> all = Lists.newArrayListWithExpectedSize(2);
            ExternalAnnotationRepository manager = getAnnotationRepository();

            Collections.addAll(all, modifierList.getAnnotations());
            if (manager != null) {
                all.addAll(manager.getAnnotations((PsiMethod) owner));
            }

            for (PsiMethod superMethod : ((PsiMethod) owner).findSuperMethods()) {
                modifierList = superMethod.getModifierList();
                Collections.addAll(all, modifierList.getAnnotations());
                if (manager != null) {
                    all.addAll(manager.getAnnotations(superMethod));
                }
            }

            //TODO ensure unique, as in EcjPsiManager
            return all.toArray(new PsiAnnotation[all.size()]);
        } else if (owner instanceof PsiClass) {
            final List<PsiAnnotation> all = Lists.newArrayListWithExpectedSize(2);
            final ExternalAnnotationRepository manager = getAnnotationRepository();

            InheritanceUtil.processSupers((PsiClass) owner, true, new Processor<PsiClass>() {
                @Override
                public boolean process(PsiClass psiClass) {
                    PsiModifierList modifierList = psiClass.getModifierList();
                    if (modifierList != null) {
                        Collections.addAll(all, modifierList.getAnnotations());
                    }
                    if (manager != null) {
                        all.addAll(manager.getAnnotations(psiClass));
                    }
                    return true;
                }
            });

            //TODO ensure unique, as in EcjPsiManager
            return all.toArray(new PsiAnnotation[all.size()]);
        } else if (owner instanceof PsiParameter) {
            PsiParameter parameter = (PsiParameter) owner;

            List<PsiAnnotation> all = Lists.newArrayListWithExpectedSize(2);
            ExternalAnnotationRepository manager = getAnnotationRepository();

            PsiMethod containingMethod = null;
            if (parameter.getParent() instanceof PsiParameterList) {
                containingMethod = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class, true);
            }

            Collections.addAll(all, modifierList.getAnnotations());

            if (containingMethod != null) {
                int parameterIndex = containingMethod.getParameterList()
                        .getParameterIndex(parameter);

                if (manager != null) {
                    all.addAll(manager.getParameterAnnotations(containingMethod, parameterIndex));
                }

                PsiClass containingClass = containingMethod.getContainingClass();
                if (containingClass != null) {
                    Set<PsiMethod> allSuperMethods = new HashSet<PsiMethod>();
                    for (PsiClass superType : InheritanceUtil.getSuperClasses(containingClass)) {
                        Collections.addAll(allSuperMethods, containingMethod.findSuperMethods(superType));
                    }

                    for (PsiMethod superMethod : allSuperMethods) {
                        PsiParameter superParameter = superMethod.getParameterList()
                                .getParameters()[parameterIndex];

                        modifierList = superParameter.getModifierList();
                        if (modifierList != null) {
                            Collections.addAll(all, modifierList.getAnnotations());
                        }

                        if (manager != null) {
                            all.addAll(manager.getParameterAnnotations(superMethod, parameterIndex));
                        }
                    }
                }
            }

            //TODO ensure unique, as in EcjPsiManager
            return all.toArray(new PsiAnnotation[all.size()]);
        } else {
            // PsiField, PsiLocalVariable etc: no inheritance
            return getDirectAnnotations(owner);
        }
    }

    @NonNull
    private static PsiAnnotation[] getDirectAnnotations(@NonNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            return modifierList.getAnnotations();
        } else {
            return PsiAnnotation.EMPTY_ARRAY;
        }
    }
}
