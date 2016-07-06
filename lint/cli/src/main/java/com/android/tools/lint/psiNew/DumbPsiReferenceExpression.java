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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;

public class DumbPsiReferenceExpression extends DumbPsiExpression implements PsiReferenceExpression {
    private PsiExpression mQualifier;
    private String mQualifiedName;
    private PsiIdentifier mIdentifier;

    public DumbPsiReferenceExpression() {
    }

    void setQualifier(PsiExpression qualifier) {
        mQualifier = qualifier;
    }

    void setNameElement(PsiIdentifier identifier) {
        mIdentifier = identifier;
    }

    @Nullable
    @Override
    public PsiExpression getQualifierExpression() {
        return mQualifier;
    }

    @Nullable
    @Override
    public PsiElement getQualifier() {
        return mQualifier;
    }

    @Nullable
    @Override
    public String getReferenceName() {
        return mIdentifier.getText();
    }

    @Override
    public boolean isQualified() {
        return mQualifier != null;
    }

    @Override
    public String getQualifiedName() {
        if (mQualifiedName == null) {
            PsiElement resolved = resolve();
            if (resolved instanceof PsiMember) {
                if (resolved instanceof PsiClass) {
                    mQualifiedName = ((PsiClass) resolved).getQualifiedName();
                } else {
                    PsiMember member = (PsiMember) resolved;
                    PsiClass containingClass = member.getContainingClass();
                    if (containingClass != null && containingClass.getQualifiedName() != null) {
                        mQualifiedName = containingClass.getQualifiedName() + '.' + member
                                .getName();
                    } else {
                        mQualifiedName = member.getName();
                    }
                }
            } else {
                mQualifiedName = getReferenceName();
            }
        }
        return mQualifiedName;
    }

    @Nullable
    @Override
    public PsiElement getReferenceNameElement() {
        return mIdentifier;
    }

    @Nullable
    @Override
    public PsiType getType() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public TextRange getRangeInElement() {
        return mIdentifier.getTextRange();
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        throw new UnimplementedLintPsiApiException();
    }

    @NonNull
    @Override
    public String getCanonicalText() {
        return getQualifiedName();
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Override
    public PsiElement bindToElementViaStaticImport(@NotNull PsiClass psiClass)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void setQualifierExpression(
            @org.jetbrains.annotations.Nullable PsiExpression psiExpression)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public PsiReferenceParameterList getParameterList() {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public PsiType[] getTypeParameters() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void processVariants(@NotNull PsiScopeProcessor psiScopeProcessor) {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public JavaResolveResult advancedResolve(boolean b) {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public JavaResolveResult[] multiResolve(boolean b) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getElement() {
        return this;
    }

    @Override
    public PsiElement handleElementRename(String s) throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement psiElement)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public boolean isReferenceTo(PsiElement psiElement) {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        throw new UnimplementedLintPsiApiException();
    }
}
