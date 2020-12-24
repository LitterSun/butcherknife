butcherknife
======
[![Platform Android](https://img.shields.io/badge/platform-Android-brightgreen)]()
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)

## butcherknife简介
一个基于ASM应用于Android开发平台的AOP框架，可作用于java源码，class文件及jar包，同时支持kotlin的应用。  
通过注解的方式进行切点的代码织入
* `@Aspect` 表明一个类是Aspect Class，且class必须是public
* `@BeforeCall` 方法调用前织入代码
* `@AfterCall` 方法调用后织入代码
* `@BeforeSuperExecute` 父类方法内部执行前织入代码，如果是子类没有重写父类方法的话，将强制实现该方法，且该方法只有只会在直接子类中只会织入一次，子类的子类不在织入，防止多次调用。
* `@AfterSuperExecute` 父类方法内部执行后织入代码

如：
```java
@Aspect
public class FragmentInjector {
    private static final String TAG = "FragmentInjector";

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
```
```java
@Aspect
public class ClickListenerInjector {
    private static final String TAG = "ClickListenerInjector";

    @BeforeSuperExecute(clazz = View.OnClickListener.class, method = "onClick")
    public static void beforeViewOnClick(View.OnClickListener listener, View view) {
        Log.e(TAG, "beforeViewOnClick: listener = " + listener + ", view = " + view);
    }
}
```

织入类必须是`public`，方法必须是 `public static`,第一个参数是切点的this对象（**如果是静态方法，该参数省略**），后面的参数分别的切点方法的参数，且除this参数外后面的参数类型是严格匹配，必须和切点方法保持一致。

## 集成步骤
在project根目录的build.gradle添加插件
```groovy
buildscript {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:x.x.x'
        classpath "com.littersun.butcherknife:butcherknife-gradle-plugin:1.0.0"
    }
}
```
然后在APP module的build.gradle应用插件
```groovy
apply plugin: 'com.littersun.butcherknife'
```

在需要的module中添加注解的依赖
```groovy
dependencies {
    implementation "com.littersun.butcherknife:butcherknife-annotations:1.0.0"
}
```

## License
```
Copyright (C) 2020 LitterSun.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

