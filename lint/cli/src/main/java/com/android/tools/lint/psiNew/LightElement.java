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
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.util.IncorrectOperationException;

public abstract class LightElement extends PsiElementBase {
  private final Language myLanguage;
  private volatile PsiElement myNavigationElement = this;

  protected LightElement(@NonNull Language language) {
    myLanguage = language;
  }

  @Override
  @NonNull
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  public PsiManager getManager() {
    return null;
  }

  @Override
  public PsiElement getParent() {
    return null;
  }

  @Override
  @NonNull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  public TextRange getTextRange() {
    return null;
  }

  @Override
  public int getStartOffsetInParent() {
    return -1;
  }

  @Override
  public final int getTextLength() {
    String text = getText();
    return text != null ? text.length() : 0;
  }

  @Override
  @NonNull
  public char[] textToCharArray() {
    return getText().toCharArray();
  }

  @Override
  public boolean textMatches(@NonNull CharSequence text) {
    return getText().equals(text.toString());
  }

  @Override
  public boolean textMatches(@NonNull PsiElement element) {
    return getText().equals(element.getText());
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return -1;
  }

  @Override
  public boolean isValid() {
    final PsiElement navElement = getNavigationElement();
    if (navElement != this) {
      return navElement.isValid();
    }

    return true;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isPhysical() {
    return false;
  }

  @Override
  public abstract String toString();

  @Override
  public void checkAdd(@NonNull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement add(@NonNull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement addBefore(@NonNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement addAfter(@NonNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement replace(@NonNull PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public ASTNode getNode() {
    return null;
  }

  @Override
  public String getText() {
    return null;
  }

  @Override
  public void accept(@NonNull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement copy() {
    return null;
  }

  @NonNull
  @Override
  public PsiElement getNavigationElement() {
    return myNavigationElement;
  }

  public void setNavigationElement(@NonNull PsiElement navigationElement) {
    PsiElement nnElement = navigationElement.getNavigationElement();
    if (nnElement != navigationElement && nnElement != null) {
      navigationElement = nnElement;
    }
    myNavigationElement = navigationElement;
  }

  @Override
  public PsiElement getPrevSibling() {
    return null;
  }

  @Override
  public PsiElement getNextSibling() {
    return null;
  }

}