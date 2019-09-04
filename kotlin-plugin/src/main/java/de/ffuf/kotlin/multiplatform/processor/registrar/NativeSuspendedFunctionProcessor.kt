package de.ffuf.kotlin.multiplatform.processor.registrar

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.ffuf.kotlin.multiplatform.annotations.NativeFlowFunction
import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction
import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.IMPORTS
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.OUTPUTDIRECTORY
import de.ffuf.kotlin.multiplatform.processor.registrar.NativeSuspendedFunctionKeys.SCOPENAME
import de.jensklingenberg.mpapt.common.*
import de.jensklingenberg.mpapt.model.AbstractProcessor
import de.jensklingenberg.mpapt.model.Element
import de.jensklingenberg.mpapt.model.RoundEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File
import kotlin.reflect.KClass

object NativeSuspendedFunctionKeys {
    val IMPORTS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("additional import statements")

    val SCOPENAME: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("scope name to use")

    val OUTPUTDIRECTORY: CompilerConfigurationKey<String> = CompilerConfigurationKey.create(
        "output directory of generated classes (excluding package name)"
    )
}
private const val TAG = "NativeSuspendedFunctionProcessor"


class NativeSuspendedFunctionProcessor : AbstractProcessor() {

    private val nativeSuspendFunction = NativeSuspendedFunction::class.java.name
    private val nativeFlowFunction = NativeFlowFunction::class.java.name

    var fileBuilder: FileSpec.Builder? = null
    var outputFile: File? = null

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

    private fun generateFunction(it: Element.FunctionElement, isFlow: Boolean) {
        log("Found suspended Function: " + it.func.name + " Module: " + it.func.module.simpleName() + " platform " + activeTargetPlatform.first().platformName)

        val packageName = it.descriptor.original.containingDeclaration.fqNameSafe.asString()
        val className = it.descriptor.defaultType.toString()
        val generatedClassName = "${className}Extensions"

        if (fileBuilder == null) {
            fileBuilder = FileSpec.builder(packageName, generatedClassName)
                .addImport("de.ffuf.kotlin.multiplatform.annotations", "suspendRunCatching")
                .addImport("kotlinx.coroutines", "launch")
                .addImport("kotlinx.coroutines.flow", "collect")
            outputFile = File(
                it.descriptor.guessingProjectFolder(), configuration.get(
                    OUTPUTDIRECTORY, "src/commonMain/kotlin"
                )
            )
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

        val returnType: TypeName = if (it.func.returnType != null) {
            val value = if (isFlow) { //kotlin.
                it.func.getReturnTypeImport().substringAfter("<").substringBefore(">").split(".")
            } else {
                it.func.getReturnTypeImport().split(".")
            }
            ClassName(value.dropLast(1).joinToString("."), value.last())
        } else {
            Unit::class.asTypeName()
        }

        fileBuilder?.addFunction(
            FunSpec.builder(it.func.name.identifier)
                .receiver(ClassName(packageName, className))
                .addAnnotations(it.func.annotations.filterNot { it.type.toString() == NativeSuspendedFunction::class.java.simpleName || it.type.toString() == NativeFlowFunction::class.java.simpleName }.map { annotation ->
                    AnnotationSpec.builder(
                        ClassName(
                            annotation.annotationClass?.original?.containingDeclaration?.fqNameSafe?.asString()
                                ?: "",
                            annotation.type.toString()
                        )
                    ).build()
                })
                .apply {
                    if (it.func.visibility == EffectiveVisibility.Internal.toVisibility()) {
                        addModifiers(KModifier.INTERNAL)
                    }
                }
                .addParameters(it.func.getFunctionParameters().map { param ->
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
            callback(it)
        }
    }
                     */

                    val scopeName = configuration.get(
                        SCOPENAME, "mainScope"
                    )
                    beginControlFlow("return $scopeName.launch {")
                    val originalCall =
                        "${it.func.name}(${it.func.getFunctionParameters().joinToString(", ") { param -> param.parameterName }})"
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
                if (!file.exists()) {
                    file.createNewFile()
                }
                it.writeTo(file)
            }

        }
    }

}
