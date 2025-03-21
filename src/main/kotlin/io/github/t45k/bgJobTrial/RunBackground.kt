package io.github.t45k.bgJobTrial

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.stereotype.Component
import kotlin.reflect.KFunction
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlin.reflect.jvm.javaMethod

interface RunBackground {
    suspend operator fun invoke(jobType: JobType, function: KSuspendFunction0<Unit>)
    suspend operator fun <T1> invoke(jobType: JobType, function: KSuspendFunction1<T1, Unit>, arg: T1)
    suspend operator fun <T1, T2> invoke(jobType: JobType, function: KSuspendFunction2<T1, T2, Unit>, arg1: T1, arg2: T2)
}

@Component
class RunBackgroundImpl(private val databaseClient: DatabaseClient) : RunBackground {
    private val objectMapper = jacksonObjectMapper()

    override suspend operator fun invoke(jobType: JobType, function: KSuspendFunction0<Unit>) {
        val serializedArgs = objectMapper.writeValueAsString(emptyList<Nothing>())
        store(jobType, function, serializedArgs)
    }

    override suspend operator fun <T1> invoke(jobType: JobType, function: KSuspendFunction1<T1, Unit>, arg: T1) {
        val serializedArgs = objectMapper.writeValueAsString(listOf(arg))
        store(jobType, function, serializedArgs)
    }

    override suspend operator fun <T1, T2> invoke(jobType: JobType, function: KSuspendFunction2<T1, T2, Unit>, arg1: T1, arg2: T2) {
        val serializedArgs = objectMapper.writeValueAsString(listOf(arg1, arg2))
        store(jobType, function, serializedArgs)
    }

    private suspend fun store(jobType: JobType, function: KFunction<*>, serializedArgs: String) {
        databaseClient.sql("insert into job(serialization_key, class_fqn, method_name, args, status) values (?, ?, ?, ?, ?)")
            .bind(0, objectMapper.writeValueAsString(jobType))
            .bind(1, function.javaMethod!!.declaringClass.canonicalName)
            .bind(2, function.name)
            .bind(3, serializedArgs)
            .bind(4, JobStatus.TODO)
            .await()
    }
}

enum class JobType {
    COMPLETE,
    INTERRUPTED,
    EXCEPTION,
}
