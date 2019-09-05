package de.ffuf.kotlin.multiplatform.processor

import org.gradle.api.Project

open class NativeSuspendFunctionsExtension {
    var scopeName = "mainScope"
    var outputDirectory = "src/commonMain/kotlin"
    var packageName: String = ""
    var imports: List<String> = emptyList()

    override fun toString(): String {
        return "NativeSuspendFunctionsExtension(scopeName='$scopeName', outputDirectory='$outputDirectory', packageName=$packageName, imports=$imports)"
    }
}

open class NativeSuspendFunctionsGradlePlugin : org.gradle.api.Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("nativeSuspendExtension", NativeSuspendFunctionsExtension::class.java)

    }

}
