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

public class SingleAnnotationVisitor extends AspectAnnotationVisitor {
    private final Class<?> mAnnotationClass;
    private String mPointcutClass;
    private String mPointcutMethod;

    public SingleAnnotationVisitor(Context context, Class<?> annotationClass) {
        super(context);
        mAnnotationClass = annotationClass;
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        switch (name) {
            case "clazz":
                mPointcutClass = value.toString();
                break;
            case "method":
                mPointcutMethod = value.toString();
                break;
            default:
                break;
        }
    }

    @Override
    public void visitEnd() {
        addPointcutAnnotation(new PointcutAnnotation(mAnnotationClass, mPointcutClass, mPointcutMethod));
        super.visitEnd();
    }
}
