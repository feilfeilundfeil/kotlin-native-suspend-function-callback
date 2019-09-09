package de.ffuf.kotlin.multiplatform.processor.registrar

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.ffuf.kotlin.multiplatform.annotations.NativeFlowFunction
import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction
import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.IMPORTS
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.OUTPUTDIRECTORY
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.PACKAGENAME
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.SCOPENAME
import de.jensklingenberg.mpapt.common.*
import de.jensklingenberg.mpapt.model.AbstractProcessor
import de.jensklingenberg.mpapt.model.Element
import de.jensklingenberg.mpapt.model.RoundEnvironment
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File

object NativeSuspendedFunctionKeys {
    val IMPORTS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("additional import statements")

    val SCOPENAME: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("scope name to use")

    val OUTPUTDIRECTORY: CompilerConfigurationKey<String> = CompilerConfigurationKey.create(
        "output directory of generated extension class (excluding package name)"
    )

    val PACKAGENAME: CompilerConfigurationKey<String> = CompilerConfigurationKey.create(
        "output package name of generated extension - if empty it uses the package name of the first found annotation"
    )
}

private const val TAG = "NativeSuspendedFunctionProcessor"


class NativeSuspendedFunctionProcessor : AbstractProcessor() {

    private val nativeSuspendFunction = NativeSuspendedFunction::class.java.name
    private val nativeFlowFunction = NativeFlowFunction::class.java.name

    private var fileBuilder: FileSpec.Builder? = null
    private var outputFile: File? = null

    override fun process(roundEnvironment: RoundEnvironment) {
        roundEnvironment.getElementsAnnotatedWith(NativeSuspendedFunction::class.java.name).forEach {
            when (it) {
                is Element.FunctionElement -> {
                    if (it.func.isSuspend) { //sanity check
                        generateFunction(it, false)
                    }
                }
            }
        }
        roundEnvironment.getElementsAnnotatedWith(NativeFlowFunction::class.java.name).forEach {
            when (it) {
                is Element.FunctionElement -> {
                    generateFunction(it, true)
                }
            }
        }
    }

    private fun generateFunction(functionElement: Element.FunctionElement, isFlow: Boolean) {
        log("Found suspended Function: " + functionElement.func.name + " Module: " + functionElement.func.module.simpleName() + " platform " + activeTargetPlatform.first().platformName)

        val packageName = configuration.get(
            PACKAGENAME, ""
        ).let {
            if (it.isEmpty()) functionElement.descriptor.original.containingDeclaration.fqNameSafe.asString() else it
        }

        val className = functionElement.descriptor.defaultType.toString()
        val generatedClassName = "NativeCoroutineExtensions"

        if (fileBuilder == null) {
            fileBuilder = FileSpec.builder(packageName, generatedClassName)
                .indent("    ")
                .addImport("de.ffuf.kotlin.multiplatform.annotations", "suspendRunCatching")
                .addImport("kotlinx.coroutines", "launch")
                .addImport("kotlinx.coroutines.flow", "collect")

            val outputDirectory = configuration.get(
                OUTPUTDIRECTORY, "src/commonMain/kotlin"
            )
            val possibleProjectDirectory = functionElement.descriptor.guessingProjectFolder()
            outputFile = File(
                // fix when trying to compile Android app
                possibleProjectDirectory.substringBefore(outputDirectory),
                outputDirectory
            )
            log("Outputfile: ${outputFile?.path} Package: $packageName")

            val imports: String? = configuration.get(IMPORTS)
            imports?.let { imports ->
                imports.split("&").forEach { import ->
                    fileBuilder?.addImport(
                        import.split(".").dropLast(1).joinToString("."),
                        import.split(".").last()
                    )
                }
            }
        }

        val originalPackageName = functionElement.descriptor.original.containingDeclaration.fqNameSafe.asString()
        fileBuilder?.addImport(originalPackageName, className)

        val returnType: TypeName = if (functionElement.func.returnType != null) {
            val value = if (isFlow) { //kotlin.
                functionElement.func.getReturnTypeImport().substringAfter("<").substringBefore(">").split(".")
            } else {
                functionElement.func.getReturnTypeImport().split(".")
            }
            ClassName(value.dropLast(1).joinToString("."), value.last())
        } else {
            Unit::class.asTypeName()
        }

        fileBuilder?.addFunction(
            FunSpec.builder(functionElement.func.name.identifier)
                .receiver(ClassName(packageName, className))
                .addAnnotations(functionElement.func.annotations.filterNot { it.type.toString() == NativeSuspendedFunction::class.java.simpleName || it.type.toString() == NativeFlowFunction::class.java.simpleName }.map { annotation ->
                    AnnotationSpec.builder(
                        ClassName(
                            annotation.annotationClass?.original?.containingDeclaration?.fqNameSafe?.asString()
                                ?: "",
                            annotation.type.toString()
                        )
                    ).addMember(annotation.allValueArguments.map { "${it.key} = ${it.value}" }.joinToString(", "))
                        .build()
                })
                .apply {
                    if (functionElement.func.visibility == EffectiveVisibility.Internal.toVisibility()) {
                        addModifiers(KModifier.INTERNAL)
                    }
                }
                .addParameters(functionElement.func.getFunctionParameters().map { param ->
                    ParameterSpec.builder(
                        param.parameterName,
                        ClassName(
                            param.packagee.packagename,
                            param.packagee.classname
                        ).copy(nullable = param.nullable)
                    ).build()
                })
                .addParameter(
                    ParameterSpec.builder(
                        "callback",
                        LambdaTypeName.get(
                            null,
                            parameters = *arrayOf(
                                if (isFlow) {
                                    returnType
                                } else {
                                    SuspendResult::class.asClassName().parameterizedBy(
                                        returnType
                                    )
                                }
                            ),
                            returnType = Unit::class.asTypeName()
                        )
                    ).build()
                )
                .addCode(buildCodeBlock {

                    /*
    @ExperimentalCoroutinesApi
    @NativeFlowFunction
    fun subscribeToMowerChanges(test: Int): Flow<String> {
        return callbackFlow {
            offer("")
        }
    }

    fun subscribeToMowerChanges(test: Int, callback: (String) -> Unit): Job = GlobalScope.launch {
        subscribeToMowerChanges(test).collect {
            callback(functionElement)
        }
    }
                     */

                    val scopeName = configuration.get(
                        SCOPENAME, "mainScope"
                    )
                    beginControlFlow("return $scopeName.launch")
                    val originalCall =
                        "${functionElement.func.name}(${functionElement.func.getFunctionParameters().joinToString(", ") { param -> param.parameterName }})"
                    if (isFlow) {
                        beginControlFlow("${originalCall}.collect")
                        addStatement("callback(it)")
                        endControlFlow()

                    } else {
                        addStatement("callback(suspendRunCatching<%T> { $originalCall })", returnType)
                    }
                    endControlFlow()
                })
                .build()
        )
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(nativeSuspendFunction, nativeFlowFunction)

    override fun processingOver() {
        log("$TAG***Processor over ***")

        fileBuilder?.build()?.let {
            outputFile?.let { file ->
                log("$TAG***Processor over, writing to $file ***")
                if (!file.exists()) {
                    file.mkdir()
                }
                it.writeTo(file)
            }

        }
    }

}
