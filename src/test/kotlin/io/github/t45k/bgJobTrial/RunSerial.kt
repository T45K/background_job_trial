package io.github.t45k.bgJobTrial

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1

@Primary
@Component
class RunSerial : RunBackground {
    override suspend fun invoke(function: KSuspendFunction0<Unit>) {
        function.invoke()
    }

    override suspend fun <T1> invoke(function: KSuspendFunction1<T1, Unit>, arg: T1) {
        function.invoke(arg)
    }
}
