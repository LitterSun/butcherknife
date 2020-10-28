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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class InjectExecuteClassVisitor extends ClassVisitor {
    private final Context mContext;
    private final Log mLog;
    private final Map<String, PointcutClass> mPointcutClassMap;
    private final List<PointcutClass> mPointcutClasses = new ArrayList<>();
    private final Set<PointcutMethod> mOverrideMethods = new HashSet<>();
    private String mCurrentClass;

    public InjectExecuteClassVisitor(ClassVisitor classVisitor, Context context, Map<String, PointcutClass> pointcutClassMap) {
        super(context.getASMVersion(), classVisitor);
        mContext = context;
        mLog = context.getLog();
        mPointcutClassMap = pointcutClassMap;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mCurrentClass = name;
        PointcutClass pointcutClass = mPointcutClassMap.get(name);
        if (pointcutClass != null) {
            mPointcutClasses.add(pointcutClass);
        }

        pointcutClass = mPointcutClassMap.get(superName);
        if (pointcutClass != null) {
            mPointcutClasses.add(pointcutClass);
        }
        for (String i : interfaces) {
            PointcutClass pointcutInterface = mPointcutClassMap.get(i);
            if (pointcutInterface != null) {
                mPointcutClasses.add(pointcutInterface);
            }
        }

        if (!mPointcutClasses.isEmpty()) {
            mContext.markModified();
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        for (PointcutClass pointcutClass : mPointcutClasses) {
            PointcutMethod pointcutMethod = pointcutClass.getPointcutMethod(name, desc);
            if (pointcutMethod != null) {
                mOverrideMethods.add(pointcutMethod);
                return new InjectMethodVisitor(mv, access, name, desc, pointcutMethod.getInjectMethods());
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        for (PointcutClass pointcutClass : mPointcutClasses) {
            for (PointcutMethod pointcutMethod : pointcutClass.getPointcutMethods()) {
                if (!mOverrideMethods.contains(pointcutMethod)) {
                    Set<InjectMethod> injectMethods = pointcutMethod.getInjectMethods();
                    Method m = new Method(pointcutMethod.getName(), pointcutMethod.getDesc());
                    GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cv);
                    for (InjectMethod injectMethod : injectMethods) {
                        if (!injectMethod.isAfter()) {
                            mg.loadThis();
                            mg.loadArgs();
                            mg.invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                            mLog.debug("Method Add: " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===ExecuteBefore===> " + mCurrentClass + "#" + pointcutMethod.getName() + pointcutMethod.getDesc());
                        }
                    }
                    mg.loadThis();
                    mg.loadArgs();
                    mg.invokeConstructor(Type.getObjectType(pointcutClass.getName()), new Method(pointcutMethod.getName(), pointcutMethod.getDesc()));
                    for (InjectMethod injectMethod : injectMethods) {
                        if (injectMethod.isAfter()) {
                            mg.loadThis();
                            mg.loadArgs();
                            mg.invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                            mLog.debug("Method Add: " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===ExecuteAfter===> " + mCurrentClass + "#" + pointcutMethod.getName() + pointcutMethod.getDesc());
                        }
                    }
                    mg.returnValue();
                    mg.endMethod();
                }
            }
        }
        super.visitEnd();
    }

    private final class InjectMethodVisitor extends AdviceAdapter {
        private final String mPointcutMethodName;
        private final String mPointcutMethodDesc;
        private final Set<InjectMethod> mInjectMethods;

        protected InjectMethodVisitor(MethodVisitor mv, int access, String name, String desc, Set<InjectMethod> injectMethods) {
            super(mContext.getASMVersion(), mv, access, name, desc);
            mPointcutMethodName = name;
            mPointcutMethodDesc = desc;
            mInjectMethods = injectMethods;
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            for (InjectMethod injectMethod : mInjectMethods) {
                if (!injectMethod.isAfter()) {
                    loadThis();
                    loadArgs();
                    invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                    mLog.debug("Method Insert: " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===ExecuteBefore===> " + mCurrentClass + "#" + mPointcutMethodName + mPointcutMethodDesc);
                }
            }
        }

        @Override
        protected void onMethodExit(int opcode) {
            for (InjectMethod injectMethod : mInjectMethods) {
                if (injectMethod.isAfter()) {
                    loadThis();
                    loadArgs();
                    invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                }
                mLog.debug("Method Insert: " + injectMethod.getClassName() + "#" + injectMethod.getMethodName() + injectMethod.getMethodDesc() + " ===ExecuteAfter===> " + mCurrentClass + "#" + mPointcutMethodName + mPointcutMethodDesc);
            }
            super.onMethodExit(opcode);
        }
    }
}
