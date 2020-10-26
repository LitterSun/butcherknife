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

package com.littersun.butcherknife.plugin.annotation;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import com.android.utils.FileUtils;
import com.littersun.butcherknife.plugin.Context;
import com.littersun.butcherknife.plugin.Log;
import com.littersun.butcherknife.plugin.annotation.visitor.AnnotationClassVisitor;
import com.littersun.butcherknife.plugin.entity.InjectMethod;
import com.littersun.butcherknife.plugin.entity.PointcutClass;
import com.littersun.butcherknife.plugin.entity.PointcutMethod;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AnnotationScanner {
    private final Collection<TransformInput> mTransformInputs;
    private final Log mLog;
    private final ClassLoader mClassLoader;

    private final Map<String, PointcutClass> mCallPointcutClasses = new HashMap<>();

    private final Map<String, PointcutClass> mExecutePointcutClasses = new HashMap<>();

    public AnnotationScanner(Collection<TransformInput> inputs, Log log, ClassLoader classLoader) {
        mTransformInputs = inputs;
        mLog = log;
        mClassLoader = classLoader;
        startScanningAnnotation();
    }

    private void startScanningAnnotation() {
        for (TransformInput input : mTransformInputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                for (File file : FileUtils.getAllFiles(directoryInput.getFile())) {
                    if (file.getName().endsWith(".class")) {
                        visitClass(file);
                    }
                }
            }

            for (JarInput jarInput : input.getJarInputs()) {
                try {
                    ZipInputStream jar = new ZipInputStream(new FileInputStream(jarInput.getFile()));
                    ZipEntry entry;
                    while ((entry = jar.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            visitClass(jar);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void visitClass(File file) {
        try {
            visitClass(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void visitClass(InputStream inputStream) {
        try {
            visitClass(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void visitClass(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(new AnnotationClassVisitor(this, new Context(mLog, mClassLoader)), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    }

    public void putCallInjectMethod(String pointcutClassName, String pointcutMethodName,
                                    String pointcutMethodDesc, String injectClassName, String injectMethodName,
                                    String injectMethodDesc, boolean isAfter) {
        mLog.info("putCallInjectMethod: pointcutClassName = " + pointcutClassName
                + ", pointcutMethodName = " + pointcutMethodName
                + ", pointcutMethodDesc = " + pointcutMethodDesc
                + ", injectClassName = " + injectClassName
                + ", injectMethodName = " + injectMethodName
                + ", injectMethodDesc = " + injectMethodDesc
                + ", isAfter = " + isAfter
        );
        putInjectMethod(mCallPointcutClasses, pointcutClassName, pointcutMethodName, pointcutMethodDesc, injectClassName, injectMethodName, injectMethodDesc, isAfter);
    }

    public void putExecuteInjectMethod(String pointcutClassName, String pointcutMethodName,
                                       String pointcutMethodDesc, String injectClassName, String injectMethodName,
                                       String injectMethodDesc, boolean isAfter) {
        mLog.info("putExecuteInjectMethod: pointcutClassName = " + pointcutClassName
                + ", pointcutMethodName = " + pointcutMethodName
                + ", pointcutMethodDesc = " + pointcutMethodDesc
                + ", injectClassName = " + injectClassName
                + ", injectMethodName = " + injectMethodName
                + ", injectMethodDesc = " + injectMethodDesc
                + ", isAfter = " + isAfter
        );
        putInjectMethod(mExecutePointcutClasses, pointcutClassName, pointcutMethodName, pointcutMethodDesc, injectClassName, injectMethodName, injectMethodDesc, isAfter);
    }

    private void putInjectMethod(Map<String, PointcutClass> classMap, String pointcutClassName,
                                 String pointcutMethodName, String pointcutMethodDesc, String injectClassName,
                                 String injectMethodName, String injectMethodDesc, boolean isAfter) {
        PointcutClass pointcutClass = classMap.get(pointcutClassName);
        if (pointcutClass == null) {
            pointcutClass = new PointcutClass(pointcutClassName);
            classMap.put(pointcutClassName, pointcutClass);
        }
        PointcutMethod pointcutMethod = pointcutClass.getPointcutMethod(pointcutMethodName, pointcutMethodDesc);
        if (pointcutMethod == null) {
            pointcutMethod = new PointcutMethod(pointcutMethodName, pointcutMethodDesc);
            pointcutClass.addPointcutMethod(pointcutMethod);
        }
        pointcutMethod.addInjectMethod(new InjectMethod(injectClassName, injectMethodName, injectMethodDesc, isAfter));
    }

    public Map<String, PointcutClass> getCallPointcutClasses() {
        return Collections.unmodifiableMap(mCallPointcutClasses);
    }

    public Map<String, PointcutClass> getExecutePointcutClasses() {
        return Collections.unmodifiableMap(mExecutePointcutClasses);
    }

}
