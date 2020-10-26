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

package com.littersun.butcherknife.plugin.inject.visitor;

import com.littersun.butcherknife.plugin.Context;
import com.littersun.butcherknife.plugin.Log;
import com.littersun.butcherknife.plugin.entity.InjectMethod;
import com.littersun.butcherknife.plugin.entity.PointcutClass;
import com.littersun.butcherknife.plugin.entity.PointcutMethod;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Map;


public class InjectCallClassVisitor extends ClassVisitor {
    private final Context mContext;
    private final Log mLog;
    private final Map<String, PointcutClass> mPointcutClasses;
    private String mCurrentClass;

    public InjectCallClassVisitor(ClassVisitor classVisitor, Context context, Map<String, PointcutClass> pointcutClasses) {
        super(context.getASMVersion(), classVisitor);
        mContext = context;
        mLog = mContext.getLog();
        mPointcutClasses = pointcutClasses;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mCurrentClass = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new AroundMethodVisitor(mv, access, name, desc);
    }

    private final class AroundMethodVisitor extends GeneratorAdapter {

        AroundMethodVisitor(MethodVisitor mv, int access, String name, String desc) {
            super(mContext.getASMVersion(), mv, access, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            PointcutMethod pointcutMethod = findPointcutMethod(owner, name, desc);

            if (pointcutMethod != null) {
                Method originalMethod = new Method(name, desc);
                int callObject = -1;
                int[] locals = new int[originalMethod.getArgumentTypes().length];
                for (int i = locals.length - 1; i >= 0; i--) {
                    locals[i] = newLocal(originalMethod.getArgumentTypes()[i]);
                    storeLocal(locals[i]);
                }
                if (opcode != Opcodes.INVOKESTATIC) {
                    callObject = newLocal(Type.getObjectType(owner));
                    storeLocal(callObject);
                }
                for (InjectMethod injectMethod : pointcutMethod.getInjectMethods()) {
                    if (!injectMethod.isAfter()) {
                        if (callObject >= 0) {
                            loadLocal(callObject);
                        }
                        for (int tmpLocal : locals) {
                            loadLocal(tmpLocal);
                        }
                        invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                        mLog.debug(mCurrentClass + ": " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===Before===> " + owner + "#" + name + desc);
                    }
                }

                if (callObject >= 0) {
                    loadLocal(callObject);
                }

                for (int tmpLocal : locals) {
                    loadLocal(tmpLocal);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);

                for (InjectMethod injectMethod : pointcutMethod.getInjectMethods()) {
                    if (injectMethod.isAfter()) {
                        if (callObject >= 0) {
                            loadLocal(callObject);
                        }
                        for (int tmpLocal : locals) {
                            loadLocal(tmpLocal);
                        }
                        invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                        mLog.debug(mCurrentClass + ": " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===After===> " + owner + "#" + name + desc);
                    }
                }
                mContext.markModified();
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

    private PointcutMethod findPointcutMethod(String className, String methodName, String methodDesc) {
        PointcutClass pointcutClass = findPointcutClass(className);
        if (pointcutClass != null) {
            return pointcutClass.getPointcutMethod(methodName, methodDesc);
        }
        return null;
    }

    private PointcutClass findPointcutClass(String className) {
        for (String clazz : mPointcutClasses.keySet()) {
            if (mContext.isAssignable(className, clazz)) {
                return mPointcutClasses.get(clazz);
            }
        }
        return null;
    }
}
