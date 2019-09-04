package de.jensklingenberg.mpapt

import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.ffuf.kotlin.multiplatform.annotations.suspendRunCatching
import kotlin.Double
import kotlin.Int
import kotlin.PublishedApi
import kotlin.Unit
import kotlin.text.Regex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@PublishedApi
internal fun CommonAnnotated.firstFunction2(
    id: Datum,
    type: Double?,
    callback: (SuspendResult<Int>) -> Unit
) = GlobalScope.launch {
    callback(suspendRunCatching<Int> { firstFunction2(id, type) })
}

fun CommonAnnotated.goToDockingStation(
    commandHandler: Regex?, callback: (SuspendResult<Regex>) ->
    Unit
) = GlobalScope.launch {
    callback(suspendRunCatching<Regex> { goToDockingStation(commandHandler) })
}

@ExperimentalCoroutinesApi
fun CommonAnnotated.testFlowFunction(test: Int, callback: (CoroutineScope) -> Unit) =
    GlobalScope.launch {
        testFlowFunction(test).collect {
            callback(it)
        }
    }
