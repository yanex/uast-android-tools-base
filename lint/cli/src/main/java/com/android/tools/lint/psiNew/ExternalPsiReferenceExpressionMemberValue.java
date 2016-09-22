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
import com.android.tools.lint.client.api.ExternalReferenceExpression;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceExpression;

public class ExternalPsiReferenceExpressionMemberValue extends DumbPsiReferenceExpression
        implements ExternalReferenceExpression
{
    private final String mFullyQualifiedName;

    public ExternalPsiReferenceExpressionMemberValue(String fullyQualifiedName) {
        mFullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public String getQualifiedName() {
        return mFullyQualifiedName;
    }

    @Nullable
    @Override
    public String getReferenceName() {
        return mFullyQualifiedName.substring(mFullyQualifiedName.lastIndexOf('.') + 1);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @Nullable
    @Override
    public PsiElement resolve(PsiElement context) {
        PsiExpression reference = JavaPsiFacade.getInstance(context.getProject())
                .getElementFactory()
                .createExpressionFromText(mFullyQualifiedName, context);

        if (reference instanceof PsiReferenceExpression) {
            return ((PsiReferenceExpression) reference).resolve();
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PsiReferenceExpression) {
            return mFullyQualifiedName.equals(((PsiReferenceExpression)o).getQualifiedName());
        } else if (o instanceof PsiField) {
            PsiField psiField = (PsiField) o;
            PsiClass containingClass = psiField.getContainingClass();
            String name = psiField.getName();
            if (containingClass != null && name != null) {
                String qualifiedName = containingClass.getQualifiedName();
                if (qualifiedName != null) {
                    return mFullyQualifiedName.length() == qualifiedName.length() + name.length() + 1
                            && mFullyQualifiedName.startsWith(qualifiedName)
                            && mFullyQualifiedName.endsWith(name);
                }
            }
        }
        return super.equals(o);
    }

    @Override
    public void accept(@NonNull PsiElementVisitor visitor) {
        throw new UnimplementedLintPsiApiException();
    }
}
