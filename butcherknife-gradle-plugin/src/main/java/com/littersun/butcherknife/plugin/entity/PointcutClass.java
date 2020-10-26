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

package com.littersun.butcherknife.plugin.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PointcutClass {
    private final String mName;
    private final Set<PointcutMethod> mPointcutMethods = new HashSet<>();

    public PointcutClass(String name) {
        mName = name;
    }

    public void addPointcutMethod(PointcutMethod method) {
        mPointcutMethods.add(method);
    }

    public String getName() {
        return mName;
    }

    public Set<PointcutMethod> getPointcutMethods() {
        return Collections.unmodifiableSet(mPointcutMethods);
    }

    public PointcutMethod getPointcutMethod(String name, String desc) {
        for (PointcutMethod method : mPointcutMethods) {
            if (name.equals(method.getName()) && desc.equals(method.getDesc())) {
                return method;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointcutClass that = (PointcutClass) o;

        return mName.equals(that.mName);
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }
}
