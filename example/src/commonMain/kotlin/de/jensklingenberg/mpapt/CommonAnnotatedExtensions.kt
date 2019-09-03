package de.jensklingenberg.mpapt

import de.ffuf.kotlin.multiplatform.annotations.SuspendResult
import de.ffuf.kotlin.multiplatform.annotations.suspendRunCatching
import kotlin.Double
import kotlin.Int
import kotlin.PublishedApi
import kotlin.Unit
import kotlin.text.Regex

@PublishedApi
internal fun CommonAnnotated.firstFunction2(
  id: Datum,
  type: Double?,
  callback: (SuspendResult<Int>) -> Unit
) = mainScope.launch {
  callback(suspendRunCatching<Int> { firstFunction2(id, type) })
}

fun CommonAnnotated.goToDockingStation(commandHandler: Regex?, callback: (SuspendResult<Regex>) ->
    Unit) = mainScope.launch {
  callback(suspendRunCatching<Regex> { goToDockingStation(commandHandler) })
}
