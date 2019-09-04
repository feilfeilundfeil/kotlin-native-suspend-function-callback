package de.ffuf.kotlin.multiplatform.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class NativeSuspendedFunction()

@Target(AnnotationTarget.FUNCTION)
annotation class NativeFlowFunction()
