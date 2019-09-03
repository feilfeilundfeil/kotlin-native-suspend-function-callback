package de.ffuf.kotlin.multiplatform.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction
import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.jensklingenberg.mpapt.common.*
import de.jensklingenberg.mpapt.model.AbstractProcessor
import de.jensklingenberg.mpapt.model.Element
import de.jensklingenberg.mpapt.model.RoundEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter




private const val TAG = "NativeSuspendedFunctionProcessor"

class NativeSuspendedFunctionProcessor(configuration: CompilerConfiguration) : AbstractProcessor(configuration) {

    private val testFunction = NativeSuspendedFunction::class.java.name
    var fileBuilder: FileSpec.Builder? = null
    var outputFile: File? = null

    override fun process(roundEnvironment: RoundEnvironment) {
        roundEnvironment.getElementsAnnotatedWith(NativeSuspendedFunction::class.java.name).forEach {
            when (it) {
                is Element.FunctionElement -> {
                    if (it.func.isSuspend) {
                        log("Found SUSPENDEDZZ Function: " + it.func.name + " Module: " + it.func.module.simpleName() + " platform   " + activeTargetPlatform.first().platformName)

                        val packageName = it.descriptor.original.containingDeclaration.fqNameSafe.asString()
                        val className = it.descriptor.defaultType.toString()
                        val generatedClassName = "${className}Extensions"

                        if (fileBuilder == null) {
                            fileBuilder = FileSpec.builder(packageName, generatedClassName)
                                .addImport("de.ffuf.kotlin.multiplatform.annotations", "suspendRunCatching")
                            outputFile = File(it.descriptor.guessingProjectFolder(), "src/commonMain/kotlin")
                            log("WRITING TO $packageName")
                        }

                        val returnType = if (it.func.returnType != null) {
                            val value = it.func.getReturnTypeImport().split(".")
                            ClassName(value.dropLast(1).joinToString("."), value.last())
                        } else {
                            Unit::class.asTypeName()
                        }
                        log("Return type: $returnType")

                        fileBuilder?.addFunction(
                            FunSpec.builder(it.func.name.identifier)
                                .receiver(ClassName(packageName, className))
                                .addAnnotations(it.func.annotations.filterNot { it.type.toString() == NativeSuspendedFunction::class.java.simpleName }.map { annotation ->
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
                                                SuspendResult::class.asClassName().parameterizedBy(
                                                    returnType
                                                )
                                            ),
                                            returnType = Unit::class.asTypeName()
                                        )
                                    ).build()
                                )
                                .addCode(buildCodeBlock {

                                    beginControlFlow("return mainScope.launch {")
                                    val originalCall =
                                        "${it.func.name}(${it.func.getFunctionParameters().joinToString(", ") { param -> param.parameterName }})"
                                    addStatement("callback(suspendRunCatching<%T> { $originalCall })", returnType)
                                    endControlFlow()
                                })
                                .build()
                        )

                    } else {
                        log("Found Function: " + it.func.name + " Module: " + it.func.module.simpleName() + " platform   " + activeTargetPlatform.first().platformName)
                    }
                }
            }
        }

    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(testFunction)

    override fun processingOver() {
        log("$TAG***Processor over ***")

        fileBuilder?.build()?.let {
            outputFile?.let { file ->
                val str = StringBuilder()
                it.writeTo(str)
                log("FILE: $file, builderzzz: ${str}")
                try {
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    it.writeTo(file)
                } catch (e: Exception) {
                    log(e.toString())
                    val errors = StringWriter()
                    e.printStackTrace(PrintWriter(errors))
                    log(errors.toString())
                }
            }

        }
    }

}
