package de.ffuf.kotlin.multiplatform.processor

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class) // don't forget!
class NativeSuspendFunctionsGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {

        val extension = project.extensions.getByType(NativeSuspendFunctionsExtension::class.java)

        return listOf(
            SubpluginOption(NativeSuspendFunctionsExtension::scopeName.name, extension.scopeName),
            SubpluginOption(NativeSuspendFunctionsExtension::outputDirectory.name, extension.outputDirectory),
            SubpluginOption(NativeSuspendFunctionsExtension::imports.name, extension.imports.joinToString("&"))
        )
    }

    override fun isApplicable(project: Project, task: AbstractCompile) =
        project.plugins.hasPlugin(NativeSuspendFunctionsGradlePlugin::class.java)


    /**
     * Just needs to be consistent with the key for CommandLineProcessor#pluginId
     */
    override fun getCompilerPluginId(): String = "native-suspend-function"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "de.ffuf.kotlin.multiplatform.processor",
        artifactId = "nativesuspendfunction-compiler",
        version = "0.0.3" // remember to bump this version before any release!
    )
}
