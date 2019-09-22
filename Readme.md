<h1 align="center">Native Suspended Functions - Generates a wrapper with a callback for suspended functions for Kotlin Native to consume</h1>

[![Kotlin](https://img.shields.io/badge/Kotlin-1.3.50-green.svg)](https://github.com/Foso/MpApt/blob/master/LICENSE)
[![License](https://img.shields.io/badge/Apache-2.0-green.svg)](https://github.com/Foso/MpApt/blob/master/LICENSE)
[ ![Download](https://api.bintray.com/packages/jonasbark/ffuf/nativesuspendfunction-compiler/images/download.svg) ](https://bintray.com/jonasbark/ffuf/nativesuspendfunction-compiler/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
  <a href="https://twitter.com/intent/tweet?text=Hey, check out Native Suspended Functions https://github.com/feilfeilundfeil/kotlin-native-suspend-function-callback via @boni2k #Kotlin 
"><img src="https://img.shields.io/twitter/url/https/github.com/angular-medellin/meetup.svg?style=social" alt="Tweet"></a>



## Introduction
As suspended functions aren't visible from Kotlin Native I created this plugin that adds support for annotating existing
functions with `@NativeSuspendedFunction`. The plugin will find these methods and generate a Kotlin source code file 
that uses callbacks that can be used from Kotlin Native.

Additionally you may use `@NativeFlowFunction` to generate a wrapper around a function that returns a Flow - something that
Kotlin Native also cannot handle right now.

This plugin uses mpapt-runtime from Jens Klingenberg: https://github.com/Foso/MpApt

## Example
Class with annotated suspended function:
```kotlin
class CommonAnnotated {

    @NativeSuspendedFunction
    internal suspend fun firstFunction2(id: Datum, type: Double?): Int {
        return 0
    }
    
    @ExperimentalCoroutinesApi
    @NativeFlowFunction
    fun subscribeToMowerChanges(test: Int): Flow<CoroutineScope> {
        return callbackFlow {
            offer(GlobalScope)
        }
    }

}
```
Generated extension:
```kotlin
internal fun CommonAnnotated.firstFunction2(
    id: Datum,
    type: Double?,
    callback: (SuspendResult<Int>) -> Unit
) = mainScope.launch {
    callback(suspendRunCatching<Int> { firstFunction2(id, type) })
}

@ExperimentalCoroutinesApi
fun CommonAnnotated.testFlowFunction(test: Int, callback: (CoroutineScope) -> Unit) =
    GlobalScope.launch {
        testFlowFunction(test).collect {
            callback(it)
        }
    }
```
The class `SuspendResult` is an implementation of https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/index.html
that was made compatible with Kotlin Native (no inline functions etc.)

Currently the plugin only supports:
- copying over annotations
- parameters + their nullability (no default values)

Please check other limitations on https://github.com/Foso/MpApt

Feel free to open tickets for new features.

## Usage

Inside your projects build.gradle(.kts) add the following plugin: 

```groovy
repositories {
    maven { url 'https://dl.bintray.com/jonasbark/ffuf' } // should be up on jcenter soon
}

plugins {
   id("native-suspend-function") version "1.0.18"
    // or as dependency: "de.ffuf.kotlin.multiplatform.processor:nativesuspendfunction:1.0.18"
}

// also add the annotations as dependency:
dependencies {
    // multiplatform binaries are ready - please have a look at the example project
    implementation("de.ffuf.kotlin.multiplatform.annotations:annotations:1.0.18")
}

```
and in your `pluginManagement`:
```kotlin
pluginManagement {
    repositories {
        // ....
        maven { url 'https://dl.bintray.com/jonasbark/ffuf' } // should be up on jcenter soon
    }
    // ....
    resolutionStrategy {
        eachPlugin {
            // ....
            if (requested.id.id == "native-suspend-function") {
                useModule("de.ffuf.kotlin.multiplatform.processor:nativesuspendfunction:${requested.version}")
            }
        }
    }
}
```

## Configuration
The compiler plugin unfortunately won't be able to resolve all needed imports but you may add them in the configuration:
```kotlin
nativeSuspendExtension {
    outputDirectory = "src/commonMain/kotlin" // this is the default output directory for the generated extension file (without package)
    scopeName = "mainScope" // in our case we use a CoroutineScope called "mainScope" - don#t forget to import that location 
    imports = listOf("test.import.package") // additional imports for the generated file
    packageName = "de.ffuf.kotlin.extensions" //allows configuring the package name of the generated file
}
```

## Release notes 
For unknown reasons, uploading the kotlin-plugin doesn't upload the buildSrc package as well.
So, publish it to mavenLocal first, then manually upload those files to:
`/de/ffuf/kotlin/multiplatform/processor/nativesuspendfunction/1.0.**` 

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE.md](https://github.com/feilfeilundfeil/kotlin-native-suspend-function-callback/blob/master/LICENSE) file for details

-------

    Copyright 2019 FFUF

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


