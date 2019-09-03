package de.ffuf.kotlin.multiplatform.processor.registrar

import com.google.auto.service.AutoService
import de.ffuf.kotlin.multiplatform.processor.NativeSuspendedFunctionKeys
import de.ffuf.kotlin.multiplatform.processor.NativeSuspendedFunctionProcessor
import de.jensklingenberg.mpapt.common.MpAptProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * This is the entry class for a compiler plugin
 */
@AutoService(ComponentRegistrar::class)
open class CommonComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        val processor = NativeSuspendedFunctionProcessor(configuration)
        val mpapt = MpAptProject(processor)
        StorageComponentContainerContributor.registerExtension(project, mpapt)
        SyntheticResolveExtension.registerExtension(project, mpapt)
        ClassBuilderInterceptorExtension.registerExtension(project, mpapt)
        JsSyntheticTranslateExtension.registerExtension(project, mpapt)
    }
}


@AutoService(CommandLineProcessor::class)
class NativeTestComponentCommandLineProcessor : CommandLineProcessor {
    companion object {
        val IMPORTS_OPTION = CliOption(
            "imports", "<fqname>", NativeSuspendedFunctionKeys.IMPORTS.toString(),
            required = false, allowMultipleOccurrences = false
        )

        val SCOPENAME_OPTION = CliOption(
            "scopeName", "<name>", NativeSuspendedFunctionKeys.SCOPENAME.toString(),
            required = false, allowMultipleOccurrences = false
        )

        val OUTPUTDIRECTORY_OPTION = CliOption(
            "outputDirectory", "<name>",
            NativeSuspendedFunctionKeys.OUTPUTDIRECTORY.toString(),
            required = false, allowMultipleOccurrences = false
        )

        val PLUGIN_ID = "native-suspend-function"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(IMPORTS_OPTION, SCOPENAME_OPTION, OUTPUTDIRECTORY_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option) {
            IMPORTS_OPTION -> configuration.put(NativeSuspendedFunctionKeys.IMPORTS, value)
            SCOPENAME_OPTION -> configuration.put(NativeSuspendedFunctionKeys.SCOPENAME, value)
            OUTPUTDIRECTORY_OPTION -> configuration.put(NativeSuspendedFunctionKeys.OUTPUTDIRECTORY, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}
