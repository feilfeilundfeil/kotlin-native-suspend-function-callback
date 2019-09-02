package de.ffuf.kotlin.multiplatform.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction
import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.jensklingenberg.mpapt.common.getFunctionParameters
import de.jensklingenberg.mpapt.common.simpleName
import de.jensklingenberg.mpapt.model.AbstractProcessor
import de.jensklingenberg.mpapt.model.Element
import de.jensklingenberg.mpapt.model.RoundEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File

private const val TAG = "NativeSuspendedFunctionProcessor"

class NativeSuspendedFunctionProcessor(configuration: CompilerConfiguration) : AbstractProcessor(configuration) {

    private val testFunction = NativeSuspendedFunction::class.java.name

    override fun process(roundEnvironment: RoundEnvironment) {
        var fileBuilder: FileSpec.Builder? = null

        roundEnvironment.getElementsAnnotatedWith(NativeSuspendedFunction::class.java.name).forEach {
            when (it) {
                is Element.FunctionElement -> {
                    if (it.func.isSuspend) {
                        log("Found SUSPENDED Function: " + it.func.name + " Module: " + it.func.module.simpleName() + " platform   " + activeTargetPlatform.first().platformName)

                        val packageName = it.descriptor.original.containingDeclaration.fqNameSafe.asString()
                        val className = it.descriptor.defaultType.toString()
                        val generatedClassName = "SuspendedExtensions"

                        if (fileBuilder == null) {
                            fileBuilder = FileSpec.builder(packageName, generatedClassName)
                                .addImport("de.ffuf.kotlin.multiplatform.annotations", "suspendRunCatching")
                        }

                        val returnType = if (it.func.returnType != null) {
                            //TODO figure out a way to get package name
                            ClassName.bestGuess(it.func.returnType!!.toString())
                        } else {
                            Unit::class.asTypeName()
                        }

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
                                        )
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
                        println("SUSPEND: ${it.func.isSuspend}")
                        log("Found Function: " + it.func.name + " Module: " + it.func.module.simpleName() + " platform   " + activeTargetPlatform.first().platformName)
                    }
                }
            }
        }

        fileBuilder?.build()?.writeTo(File("example/src/commonMain/kotlin"))
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(testFunction)

    override fun processingOver() {
        log("$TAG***Processor over ***")
    }

}
