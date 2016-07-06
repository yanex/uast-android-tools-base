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
import com.google.common.base.Objects;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.meta.PsiMetaData;

public class ExternalPsiAnnotation extends DumbPsiElement implements PsiAnnotation, PsiAnnotationParameterList {
    @NonNull
    private final String mSignature;
    
    public ExternalPsiAnnotation(@NonNull String signature) {
        mSignature = signature;
    }

    private PsiNameValuePair[] mAttributes = PsiNameValuePair.EMPTY_ARRAY;

    public void setAttributes(PsiNameValuePair[] attributes) {
        mAttributes = attributes;
    }

    @NonNull
    @Override
    public PsiNameValuePair[] getAttributes() {
        return mAttributes;
    }

    @NonNull
    @Override
    public PsiAnnotationParameterList getParameterList() {
        return this;
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return mSignature;
    }

    @Nullable
    @Override
    public PsiJavaCodeReferenceElement getNameReferenceElement() {
        return null;
    }

    @Nullable
    @Override
    public PsiAnnotationMemberValue findAttributeValue(String s) {
        for (PsiNameValuePair pair : getParameterList().getAttributes()) {
            if (Objects.equal(pair.getName(), s)) {
                return pair.getValue();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public PsiAnnotationMemberValue findDeclaredAttributeValue(String s) {
        return findAttributeValue(s);
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getOwner() {
        return null;
    }

    @Override
    public void accept(@NonNull PsiElementVisitor visitor) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void acceptChildren(@NonNull PsiElementVisitor visitor) {
        throw new UnimplementedLintPsiApiException();
    }

    @Nullable
    @Override
    public PsiMetaData getMetaData() {
        return null;
    }

    @Override
    public <T extends PsiAnnotationMemberValue> T setDeclaredAttributeValue(
            @org.jetbrains.annotations.Nullable String s,
            @org.jetbrains.annotations.Nullable T t) {
        throw new UnimplementedLintPsiApiException();
    }
}
