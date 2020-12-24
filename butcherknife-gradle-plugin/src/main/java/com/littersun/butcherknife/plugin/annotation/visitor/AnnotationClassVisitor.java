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

import com.littersun.butcherknife.annotations.AfterCall;
import com.littersun.butcherknife.annotations.AfterCalls;
import com.littersun.butcherknife.annotations.AfterSuperExecute;
import com.littersun.butcherknife.annotations.AfterSuperExecutes;
import com.littersun.butcherknife.annotations.Aspect;
import com.littersun.butcherknife.annotations.BeforeCall;
import com.littersun.butcherknife.annotations.BeforeCalls;
import com.littersun.butcherknife.annotations.BeforeSuperExecute;
import com.littersun.butcherknife.annotations.BeforeSuperExecutes;
import com.littersun.butcherknife.plugin.Context;
import com.littersun.butcherknife.plugin.annotation.AnnotationScanner;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class AnnotationClassVisitor extends ClassVisitor {
    private final AnnotationScanner mAnnotationScanner;
    private final Context mContext;
    private boolean mIsAspectClass = false;

    public AnnotationClassVisitor(AnnotationScanner annotationScanner, Context context) {
        super(context.getASMVersion());
        mAnnotationScanner = annotationScanner;
        mContext = context;
    }

    public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
        mContext.setClassName(name);
        super.visit(version, access, name, sig, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(Type.getType(Aspect.class).getDescriptor())) {
            System.err.println("Find Aspect class is " + mContext.getClassName());
            mIsAspectClass = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (mIsAspectClass) {
            return new MethodAnnotationScanner(name, desc);
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class MethodAnnotationScanner extends MethodVisitor {
        private final String mInjectMethodName;
        private final String mInjectMethodDesc;
        private AspectAnnotationVisitor mAnnotationVisitor;

        private MethodAnnotationScanner(String methodName, String methodDesc) {
            super(mContext.getASMVersion());
            mInjectMethodName = methodName;
            mInjectMethodDesc = methodDesc;
        }


        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.equals(Type.getType(AfterCall.class).getDescriptor())) {
                mAnnotationVisitor = new SingleAnnotationVisitor(mContext, AfterCall.class);
            } else if (desc.equals(Type.getType(AfterCalls.class).getDescriptor())) {
                mAnnotationVisitor = new RepeatableAnnotationVisitor(mContext, AfterCall.class);
            } else if (desc.equals(Type.getType(BeforeCall.class).getDescriptor())) {
                mAnnotationVisitor = new SingleAnnotationVisitor(mContext, BeforeCall.class);
            } else if (desc.equals(Type.getType(BeforeCalls.class).getDescriptor())) {
                mAnnotationVisitor = new RepeatableAnnotationVisitor(mContext, BeforeCall.class);
            } else if (desc.equals(Type.getType(AfterSuperExecute.class).getDescriptor())) {
                mAnnotationVisitor = new SingleAnnotationVisitor(mContext, AfterSuperExecute.class);
            } else if (desc.equals(Type.getType(AfterSuperExecutes.class).getDescriptor())) {
                mAnnotationVisitor = new RepeatableAnnotationVisitor(mContext, AfterSuperExecute.class);
            } else if (desc.equals(Type.getType(BeforeSuperExecute.class).getDescriptor())) {
                mAnnotationVisitor = new SingleAnnotationVisitor(mContext, BeforeSuperExecute.class);
            } else if (desc.equals(Type.getType(BeforeSuperExecutes.class).getDescriptor())) {
                mAnnotationVisitor = new RepeatableAnnotationVisitor(mContext, BeforeSuperExecute.class);
            }
            if (mAnnotationVisitor != null) {
                return mAnnotationVisitor;
            }
            return super.visitAnnotation(desc, visible);
        }

        private boolean isAfterAnnotation(Class<?> annotation) {
            return annotation == AfterCall.class
                    || annotation == AfterSuperExecute.class;
        }

        @Override
        public void visitEnd() {
            if (mAnnotationVisitor != null) {
                for (PointcutAnnotation pointcutAnnotation : mAnnotationVisitor.getPointcutAnnotations()) {
                    try {
                        String pointcutMethodDesc = getPointcutMethodDesc(pointcutAnnotation.clazz, pointcutAnnotation.method);
                        if (pointcutMethodDesc == null) {
                            throw new RuntimeException("pointcutClassName = " + pointcutAnnotation.clazz + ", pointcutMethodName = " + pointcutAnnotation.method + ", pointcutMethodDesc is NULL");
                        }

                        if (pointcutAnnotation.annotation == AfterCall.class
                                || pointcutAnnotation.annotation == BeforeCall.class) {
                            mAnnotationScanner.putCallInjectMethod(Type.getType(pointcutAnnotation.clazz).getInternalName(), pointcutAnnotation.method,
                                    pointcutMethodDesc, mContext.getClassName(), mInjectMethodName, mInjectMethodDesc,
                                    isAfterAnnotation(pointcutAnnotation.annotation));
                        } else if (pointcutAnnotation.annotation == AfterSuperExecute.class
                                || pointcutAnnotation.annotation == BeforeSuperExecute.class) {
                            mAnnotationScanner.putSuperExecuteInjectMethod(Type.getType(pointcutAnnotation.clazz).getInternalName(), pointcutAnnotation.method,
                                    pointcutMethodDesc, mContext.getClassName(), mInjectMethodName, mInjectMethodDesc,
                                    isAfterAnnotation(pointcutAnnotation.annotation));
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            super.visitEnd();
        }

        private String getPointcutMethodDescStart(String methodName, String methodDesc) {
            Method method = new Method(methodName, methodDesc);
            Type[] argumentTypes = method.getArgumentTypes();
            if (argumentTypes.length <= 1) {
                return "()";
            }
            Type[] pointcutArgumentTypes = new Type[argumentTypes.length - 1];
            System.arraycopy(argumentTypes, 1, pointcutArgumentTypes, 0, pointcutArgumentTypes.length);

            method = new Method(methodName, Type.getType(void.class), pointcutArgumentTypes);
            String descriptor = method.getDescriptor();
            return descriptor.substring(0, descriptor.length() - 1);
        }


        private String getPointcutMethodDesc(String pointcutClass, String pointcutMethodName) throws ClassNotFoundException {
            Class<?> aClass = mContext.getClassLoader().loadClass(Type.getType(pointcutClass).getClassName());
            while (aClass != Object.class) {
                String objectPointcutMethodDescStart = getPointcutMethodDescStart(mInjectMethodName, mInjectMethodDesc);
                String staticPointcutMethodDescStart = mInjectMethodDesc.substring(0, mInjectMethodDesc.length() - 1);
                for (java.lang.reflect.Method declaredMethod : aClass.getDeclaredMethods()) {
                    Method method = Method.getMethod(declaredMethod);
                    if (method.getName().equals(pointcutMethodName) &&
                            (method.getDescriptor().startsWith(objectPointcutMethodDescStart) || method.getDescriptor().startsWith(staticPointcutMethodDescStart))) {
                        return method.getDescriptor();
                    }
                }
                aClass = aClass.getSuperclass();
            }

            return null;
        }
    }

}
