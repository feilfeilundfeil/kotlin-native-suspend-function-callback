package de.jensklingenberg.mpapt

import de.ffuf.kotlin.multiplatform.annotations.NativeSuspendedFunction


class CommonAnnotated constructor() {

    @NativeSuspendedFunction
    @PublishedApi
    internal suspend fun firstFunction2(id: Int): Int {
        return 0
    }
}
