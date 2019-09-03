package de.jensklingenberg.mpapt

import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction

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

}
