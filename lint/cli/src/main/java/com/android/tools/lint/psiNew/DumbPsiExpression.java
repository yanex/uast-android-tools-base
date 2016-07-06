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

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class DumbPsiExpression implements PsiExpression {
    @Override
    public PsiManager getManager() {
        return null;
    }

    @NotNull
    @Override
    public Project getProject() throws PsiInvalidElementAccessException {
        return null;
    }

    @Override
    public PsiElement getParent() {
        return null;
    }

    @Nullable
    @Override
    public PsiType getType() {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @NotNull
    @Override
    public PsiElement[] getChildren() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getFirstChild() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getLastChild() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getNextSibling() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getPrevSibling() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public TextRange getTextRange() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public int getStartOffsetInParent() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return 0;
    }

    @Nullable
    @Override
    public PsiElement findElementAt(int i) {
        return null;
    }

    @Nullable
    @Override
    public PsiReference findReferenceAt(int i) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @Override
    public String getText() {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public char[] textToCharArray() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement getNavigationElement() {
        return null;
    }

    @Override
    public PsiElement getOriginalElement() {
        return this;
    }

    @Override
    public boolean textMatches(@NotNull CharSequence charSequence) {
        return false;
    }

    @Override
    public boolean textMatches(@NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean textContains(char c) {
        return false;
    }

    @Override
    public void accept(@NotNull PsiElementVisitor psiElementVisitor) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void acceptChildren(@NotNull PsiElementVisitor psiElementVisitor) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement copy() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement add(@NotNull PsiElement psiElement) throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement addBefore(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement addAfter(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void checkAdd(@NotNull PsiElement psiElement) throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement addRange(PsiElement psiElement, PsiElement psiElement1)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement addRangeBefore(@NotNull PsiElement psiElement,
            @NotNull PsiElement psiElement1, PsiElement psiElement2)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement addRangeAfter(PsiElement psiElement, PsiElement psiElement1,
            PsiElement psiElement2) throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void delete() throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {

    }

    @Override
    public void deleteChildRange(PsiElement psiElement, PsiElement psiElement1)
            throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public PsiElement replace(@NotNull PsiElement psiElement) throws IncorrectOperationException {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Nullable
    @Override
    public PsiReference getReference() {
        return null;
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return new PsiReference[0];
    }

    @Nullable
    @Override
    public <T> T getCopyableUserData(Key<T> key) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public <T> void putCopyableUserData(Key<T> key, @Nullable T t) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor psiScopeProcessor,
            @NotNull ResolveState resolveState, @Nullable PsiElement psiElement,
            @NotNull PsiElement psiElement1) {
        throw new UnimplementedLintPsiApiException();
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public boolean isPhysical() {
        return false;
    }

    @NotNull
    @Override
    public GlobalSearchScope getResolveScope() {
        throw new UnimplementedLintPsiApiException();
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public ASTNode getNode() {
        return null;
    }

    @Override
    public boolean isEquivalentTo(PsiElement psiElement) {
        return this == psiElement;
    }

    @Override
    public Icon getIcon(@IconFlags int i) {
        throw new UnimplementedLintPsiApiException();
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        throw new UnimplementedLintPsiApiException();
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
        throw new UnimplementedLintPsiApiException();
    }
}
