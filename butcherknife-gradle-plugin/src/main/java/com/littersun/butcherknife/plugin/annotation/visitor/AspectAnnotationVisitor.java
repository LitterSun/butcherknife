/*
 * Copyright (C) 2020 LitterSun.
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

package com.littersun.butcherknife.plugin.annotation.visitor;

import com.littersun.butcherknife.plugin.Context;

import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class AspectAnnotationVisitor extends AnnotationVisitor {
    private final Context mContext;
    private final List<PointcutAnnotation> mPointcutAnnotations = new ArrayList<>();

    public AspectAnnotationVisitor(Context context) {
        super(context.getASMVersion());
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public List<PointcutAnnotation> getPointcutAnnotations() {
        return mPointcutAnnotations;
    }

    protected void addPointcutAnnotation(PointcutAnnotation annotation) {
        mPointcutAnnotations.add(annotation);
    }

    protected void addPointcutAnnotations(List<PointcutAnnotation> annotations) {
        mPointcutAnnotations.addAll(annotations);
    }
}
