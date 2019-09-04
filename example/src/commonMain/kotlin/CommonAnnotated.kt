package de.jensklingenberg.mpapt

import de.ffuf.kotlin.multiplatform.annotations.NativeFlowFunction
import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

typealias Datum = CharProgression

class CommonAnnotated {

    @NativeSuspendedFunction
    @PublishedApi
    internal suspend fun firstFunction2(id: Datum, type: Double?): Int {
        return 0
    }

    @NativeSuspendedFunction
    suspend fun goToDockingStation(commandHandler: Regex?) =
        Regex("")

    @ExperimentalCoroutinesApi
    @NativeFlowFunction
    fun testFlowFunction(test: Int): Flow<CoroutineScope> {
        return callbackFlow {
            offer(GlobalScope)
        }
    }
}
