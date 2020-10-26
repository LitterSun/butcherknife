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

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ButcherKnifePlugin implements Plugin<Project> {
    private Logger mLogger;

    @Override
    public void apply(Project target) {
        mLogger = target.getLogger();
        AppExtension android = target.getExtensions().findByType(AppExtension.class);
        target.getExtensions().create("butcherknife", PluginExtension.class);

        ButcherKnifeTransform transform = new ButcherKnifeTransform(target);
        android.registerTransform(transform);

        target.afterEvaluate(project -> {
            checkJavaVersion();
            onGotAndroidJarFiles(android, transform);
        });
    }

    private void checkJavaVersion() {
        String version = System.getProperty("java.version");
        Matcher matcher = Pattern.compile("^(1\\.[0-9]+)\\..*").matcher(version);
        if (matcher.find()) {
            String versionNum = matcher.group(1);
            try {
                int num = (int) (Float.parseFloat(versionNum) * 10);
                if (num < 18) {
                    throw new RuntimeException("ButcherKnifePlugin 要求编译环境的JDK为1.8及以上");
                }
            } catch (NumberFormatException e) {
                // ignore
            }
            return;
        }
        mLogger.info("ButcherKnifePlugin: check java version failed");
    }

    private void onGotAndroidJarFiles(AppExtension appExtension, ButcherKnifeTransform transform) {
        try {
            List<File> files = appExtension.getBootClasspath();
            if (files == null || files.isEmpty()) {
                throw new RuntimeException("ButcherKnifePlugin: get android.jar failed");
            }
            List<URL> androidJars = new ArrayList<>();
            for (File file : files) {
                androidJars.add(file.toURL());
            }
            transform.setAndroidJars(androidJars);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ButcherKnifePlugin: get android.jar failed");
        }
    }
}
