<h1 align="center">MpApt - Kotlin (Native/JS/JVM) Annotation Processor library</h1>

[![jCenter](https://img.shields.io/badge/Kotlin-1.3.50-green.svg
)](https://github.com/Foso/MpApt/blob/master/LICENSE)[![jCenter](https://img.shields.io/badge/Apache-2.0-green.svg)](https://github.com/Foso/MpApt/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![All Contributors](https://img.shields.io/badge/all_contributors-1-range.svg?style=flat-square)](#contributors)
  <a href="https://twitter.com/intent/tweet?text=Hey, check out MpApt https://github.com/Foso/MpApt via @jklingenberg_ #Android 
"><img src="https://img.shields.io/twitter/url/https/github.com/angular-medellin/meetup.svg?style=social" alt="Tweet"></a>



## Introduction 🙋‍♂️ 🙋‍
As suspended functions aren't visible from Kotlin Native I created this plugin that adds support for annotating existing
functions with `@NativeSuspendedFunction`. The plugin will find these methods and generate a Kotlin source code file 
that uses callbacks that can be used from Kotlin Native.

This plugin uses mpapt-runtime from Jens Klingenberg: https://github.com/Foso/MpApt

## Usage

Inside your compiler plugin, add the dependency from MavenCentral 

```groovy
repositories {
    mavenCentral()
}

dependencies {
   compile 'de.ffuf.kotlin.multiplatform.annotation:mpapt-runtime:0.8.0'
}
```


## 📜 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE.md](https://github.com/Foso/MpApt/blob/master/LICENSE) file for details

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


