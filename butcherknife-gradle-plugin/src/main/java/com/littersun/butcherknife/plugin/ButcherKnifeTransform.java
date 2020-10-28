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

import com.android.annotations.Nullable;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.littersun.butcherknife.plugin.annotation.AnnotationScanner;
import com.littersun.butcherknife.plugin.inject.visitor.ContextClassVisitor;
import com.littersun.butcherknife.plugin.inject.visitor.DesugaredClassVisitor;
import com.littersun.butcherknife.plugin.inject.visitor.DesugaringClassVisitor;
import com.littersun.butcherknife.plugin.inject.visitor.InjectCallClassVisitor;
import com.littersun.butcherknife.plugin.inject.visitor.InjectExecuteClassVisitor;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ButcherKnifeTransform extends Transform {
    private final PluginExtension mPluginExtension;
    private Log mLog;
    private List<URL> mAndroidJars;

    private ClassLoader mClassLoader;
    private AnnotationScanner mAnnotationScanner;
    private String[] mUserExcludePackages;

    public ButcherKnifeTransform(Project project) {
        mPluginExtension = project.getExtensions().getByType(PluginExtension.class);
    }

    @Override
    public String getName() {
        return "butcherknife";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    public void setAndroidJars(List<URL> androidJars) {
        mAndroidJars = androidJars;
    }

    private void setUserExcludePackages(String[] packages) {
        if (packages == null) {
            mUserExcludePackages = new String[]{};
        } else {
            mUserExcludePackages = new String[packages.length];
            for (int i = 0; i < packages.length; i++) {
                mUserExcludePackages[i] = packages[i].replace('.', '/');
            }
        }
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        if (mPluginExtension.isLogEnabled()) {
            mLog = new SystemLog();
        } else {
            mLog = new ErrorLog();
        }
        setUserExcludePackages(mPluginExtension.getExcludePackages());

        mLog.info("transform task start: " + "Transform = " + getClass().getSimpleName() + ", isIncremental = " + transformInvocation.isIncremental());

        ArrayList<URL> urlList = new ArrayList<>();
        for (TransformInput input : transformInvocation.getInputs()) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                urlList.add(directoryInput.getFile().toURL());
            }

            for (JarInput jarInput : input.getJarInputs()) {
                urlList.add(jarInput.getFile().toURL());
            }
        }
        urlList.addAll(mAndroidJars);
        URL[] urlArray = new URL[urlList.size()];
        urlList.toArray(urlArray);
        mClassLoader = new URLClassLoader(urlArray);

        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();

        if (!transformInvocation.isIncremental()) {
            outputProvider.deleteAll();
        }

        mAnnotationScanner = new AnnotationScanner(transformInvocation.getInputs(), mLog, mClassLoader);

        for (TransformInput input : transformInvocation.getInputs()) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                for (File file : FileUtils.getAllFiles(directoryInput.getFile())) {
                    File outDir = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                    outDir.mkdirs();
                    final String outDirPath = outDir.getAbsolutePath();
                    final String inputDirPath = directoryInput.getFile().getAbsolutePath();
                    final String relativeClassPath = file.getAbsolutePath().substring(inputDirPath.length());
                    File outFile = new File(outDirPath, relativeClassPath);
                    if (relativeClassPath.endsWith(".class")) {
                        transformClassFile(file, outFile);
                    } else {
                        FileUtils.copyFile(file, outFile);
                    }
                }
            }

            for (JarInput jarInput : input.getJarInputs()) {
                File jarOut = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                jarOut.getParentFile().mkdirs();
                if (jarOut.exists()) {
                    jarOut.delete();
                }
                try (ZipOutputStream outJar = new ZipOutputStream(new FileOutputStream(jarOut))) {
                    try (ZipInputStream jar = new ZipInputStream(new FileInputStream(jarInput.getFile()))) {
                        ZipEntry entry;
                        while ((entry = jar.getNextEntry()) != null) {
                            ZipEntry outEntry = copyEntry(entry);
                            outJar.putNextEntry(outEntry);
                            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                transformClass(jar, outJar);
                            } else {
                                IOUtils.copy(jar, outJar);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ZipEntry copyEntry(ZipEntry entry) {
        ZipEntry newEntry = new ZipEntry(entry.getName());
        newEntry.setComment(entry.getComment());
        newEntry.setExtra(entry.getExtra());
        return newEntry;
    }

    public boolean transformClassFile(File from, File to) {
        boolean result;
        File toParent = to.getParentFile();
        toParent.mkdirs();
        try (FileInputStream fileInputStream = new FileInputStream(from); FileOutputStream fileOutputStream = new FileOutputStream(to)) {
            result = transformClass(fileInputStream, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public boolean transformClass(InputStream from, OutputStream to) {
        try {
            byte[] bytes = IOUtils.toByteArray(from);
            byte[] modifiedClass = visitClassBytes(bytes);
            if (modifiedClass != null) {
                IOUtils.write(modifiedClass, to);
                return true;
            } else {
                IOUtils.write(bytes, to);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isExcludedPackage(String packageName) {
        for (String exPackage : mUserExcludePackages) {
            if (packageName.startsWith(exPackage)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private byte[] visitClassBytes(byte[] bytes) {
        String className = null;
        try {
            ClassReader classReader = new ClassReader(bytes);
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            Context context = new Context(mLog, mClassLoader);
            classReader.accept(new ContextClassVisitor(context), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            className = context.getClassName();
            ClassVisitor classVisitor;
            if (isExcludedPackage(context.getClassName())) {
                return null;
            }
            DesugaringClassVisitor desugaringClassVisitor = new DesugaringClassVisitor(
                    new InjectCallClassVisitor(
                            new InjectExecuteClassVisitor(classWriter, context, mAnnotationScanner.getExecutePointcutClasses()),
                            context, mAnnotationScanner.getCallPointcutClasses()),
                    context, mAnnotationScanner.getExecutePointcutClasses());
            classVisitor = desugaringClassVisitor;
            classReader.accept(classVisitor, ClassReader.SKIP_FRAMES | ClassReader.EXPAND_FRAMES);
            if (!desugaringClassVisitor.getPointcutMethods().isEmpty()) {
                // lambda 表达式需要特殊处理两次
                mLog.debug("Deal with lambda second time: " + className);
                ClassReader lambdaReader = new ClassReader(classWriter.toByteArray());
                classWriter = new ClassWriter(lambdaReader, ClassWriter.COMPUTE_MAXS);
                lambdaReader.accept(new DesugaredClassVisitor(classWriter, context, desugaringClassVisitor.getPointcutMethods()), ClassReader.SKIP_FRAMES | ClassReader.EXPAND_FRAMES);
            }
            if (context.isClassModified()) {
                return classWriter.toByteArray();
            }
        } catch (Throwable t) {
            this.mLog.error("Unfortunately, an error has occurred while processing " + className + ".\n" + t.getMessage(), t);
        }
        return null;
    }
}
