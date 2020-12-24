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

package com.littersun.butcherknife.aspect;

import android.util.Log;

import com.littersun.butcherknife.StaticMethodClass;
import com.littersun.butcherknife.annotations.AfterCall;
import com.littersun.butcherknife.annotations.Aspect;
import com.littersun.butcherknife.annotations.BeforeCall;

@Aspect
public class StaticMethodInjector {
    private static final String TAG = "StaticMethodInjector";

    private StaticMethodInjector() {
    }

    @BeforeCall(clazz = StaticMethodClass.class, method = "unnamedMethod")
    public static void beforeUnnamedMethod(long l, String s, int i) {
        Log.e(TAG, "beforeUnnamedMethod: l = " + l + ", s = " + s + ", i = " + i);
    }

    @AfterCall(clazz = StaticMethodClass.class, method = "privateStaticMethod")
    public static void afterPrivateStaticMethod(long l, String s, int i) {
        Log.e(TAG, "afterPrivateStaticMethod: l = " + l + ", s = " + s + ", i = " + i);
    }
}
