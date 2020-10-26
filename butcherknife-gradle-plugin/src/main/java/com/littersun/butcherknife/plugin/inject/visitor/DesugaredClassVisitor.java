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
import com.littersun.butcherknife.plugin.entity.PointcutMethod;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Set;

public class DesugaredClassVisitor extends ClassVisitor {
    private final Context mContext;
    private final Log mLog;
    private final Set<PointcutMethod> mPointcutMethods;

    public DesugaredClassVisitor(ClassVisitor cv, Context context, Set<PointcutMethod> pointcutMethods) {
        super(context.getASMVersion(), cv);
        mContext = context;
        mLog = context.getLog();
        mPointcutMethods = pointcutMethods;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        return new DesugaredMethodVisitor(mContext.getASMVersion(), methodVisitor, access, name, desc);
    }

    private PointcutMethod findPointcutMethod(String name, String desc) {
        if (mPointcutMethods.isEmpty()) {
            return null;
        }

        for (PointcutMethod pointcutMethod : mPointcutMethods) {
            if (name.equals(pointcutMethod.getName()) && desc.equals(pointcutMethod.getDesc())) {
                return pointcutMethod;
            }
        }
        return null;
    }

    private final class DesugaredMethodVisitor extends AdviceAdapter {
        private final String mName;
        private final String mDesc;

        protected DesugaredMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            mName = name;
            mDesc = desc;
        }

        @Override
        protected void onMethodEnter() {
            PointcutMethod pointcutMethod = findPointcutMethod(mName, mDesc);
            if (pointcutMethod == null) {
                return;
            }

            for (InjectMethod injectMethod : pointcutMethod.getInjectMethods()) {
                if (!injectMethod.isAfter()) {
                    visitInsn(ACONST_NULL);
                    int injectArgsLen = Type.getArgumentTypes(injectMethod.getMethodDesc()).length - 1;
                    int originArgsLen = Type.getArgumentTypes(mDesc).length;
                    if (injectArgsLen != 0) {
                        loadArgs(originArgsLen - injectArgsLen, injectArgsLen);
                    }
                    invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                }
            }
            mContext.markModified();
        }

        @Override
        protected void onMethodExit(int opcode) {
            PointcutMethod pointcutMethod = findPointcutMethod(mName, mDesc);
            if (pointcutMethod == null) {
                return;
            }
            for (InjectMethod injectMethod : pointcutMethod.getInjectMethods()) {
                if (injectMethod.isAfter()) {
                    visitInsn(ACONST_NULL);
                    int injectArgsLen = Type.getArgumentTypes(injectMethod.getMethodDesc()).length - 1;
                    int originArgsLen = Type.getArgumentTypes(mDesc).length;
                    if (injectArgsLen != 0) {
                        loadArgs(originArgsLen - injectArgsLen, injectArgsLen);
                    }
                    invokeStatic(Type.getObjectType(injectMethod.getClassName()), new Method(injectMethod.getMethodName(), injectMethod.getMethodDesc()));
                }
            }
            mPointcutMethods.remove(pointcutMethod);
            mContext.markModified();
        }
    }
}
