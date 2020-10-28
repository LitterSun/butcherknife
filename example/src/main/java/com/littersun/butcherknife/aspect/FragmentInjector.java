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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;

import com.littersun.butcherknife.annotations.AfterSuperExecute;
import com.littersun.butcherknife.annotations.Aspect;
import com.littersun.butcherknife.annotations.BeforeCall;
import com.littersun.butcherknife.annotations.BeforeSuperExecute;

@Aspect
public class FragmentInjector {
    private static final String TAG = "FragmentInjector";

    private FragmentInjector() {
    }

    @BeforeCall(clazz = FragmentTransaction.class, method = "replace")
    public static void beforeCallFragmentReplace(FragmentTransaction transaction, int containerViewId, Fragment fragment) {
        Log.e(TAG, "beforeCallFragmentReplace: transaction = " + transaction + ", containerViewId = " + containerViewId + " ,fragment = " + fragment);
    }

    @BeforeSuperExecute(clazz = Fragment.class, method = "onCreate")
    public static void beforeFragmentCreate(Fragment fragment, Bundle savedInstanceState) {
        Log.e(TAG, "beforeFragmentCreate: fragment = " + fragment + ", savedInstanceState = " + savedInstanceState);
    }

    @AfterSuperExecute(clazz = Fragment.class, method = "onResume")
    @AfterSuperExecute(clazz = DialogFragment.class, method = "onResume")
    @AfterSuperExecute(clazz = ListFragment.class, method = "onResume")
    public static void afterFragmentResume(Fragment fragment) {
        Log.e(TAG, "afterFragmentResume: fragment = " + fragment);
    }
}
