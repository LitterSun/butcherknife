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

class RepeatableAnnotationVisitor extends AspectAnnotationVisitor {
    private final Class<?> mAnnotationClass;

    RepeatableAnnotationVisitor(Context context, Class<?> afterClass) {
        super(context);
        mAnnotationClass = afterClass;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        if ("value".equals(name)) {
            return new ArrayAnnotationVisitor();
        }

        return super.visitArray(name);
    }

    private class ArrayAnnotationVisitor extends AnnotationVisitor {
        private final List<SingleAnnotationVisitor> mVisitors = new ArrayList<>();

        private ArrayAnnotationVisitor() {
            super(RepeatableAnnotationVisitor.this.getContext().getASMVersion());
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            SingleAnnotationVisitor visitor = new SingleAnnotationVisitor(RepeatableAnnotationVisitor.this.getContext(), mAnnotationClass);
            mVisitors.add(visitor);
            return visitor;
        }

        @Override
        public void visitEnd() {
            for (SingleAnnotationVisitor visitor : mVisitors) {
                RepeatableAnnotationVisitor.this.addPointcutAnnotations(visitor.getPointcutAnnotations());
            }
            super.visitEnd();
        }
    }
}
