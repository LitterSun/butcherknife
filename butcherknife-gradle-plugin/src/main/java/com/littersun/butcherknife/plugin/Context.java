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

package com.littersun.butcherknife.plugin;

import org.objectweb.asm.Opcodes;

public class Context {
    private final Log mLog;
    private final ClassLoader mClassLoader;
    private String mClassName;
    private boolean mClassModified;

    public Context(Log log, ClassLoader classLoader) {
        mLog = log;
        mClassLoader = classLoader;
    }

    public int getASMVersion() {
        return Opcodes.ASM6;
    }

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String className) {
        mClassName = className;
    }

    public Log getLog() {
        return mLog;
    }

    public ClassLoader getClassLoader() {
        return mClassLoader;
    }

    public boolean isClassModified() {
        return mClassModified;
    }

    public void markModified() {
        mClassModified = true;
    }

    public boolean isAssignable(String subClassName, String superClassName) {
        if (subClassName.contains("/")) {
            subClassName = subClassName.replace("/", ".");
        }
        if (superClassName.contains("/")) {
            superClassName = superClassName.replace("/", ".");
        }
        try {
            Class<?> subClass = mClassLoader.loadClass(subClassName);
            Class<?> superClass = mClassLoader.loadClass(superClassName);
            return superClass.isAssignableFrom(subClass);
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }
}
