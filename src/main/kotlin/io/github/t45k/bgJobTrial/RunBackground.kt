package io.github.t45k.bgJobTrial

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.stereotype.Component
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.jvm.javaMethod

interface RunBackground {
    suspend operator fun invoke(function: KSuspendFunction0<Unit>)
    suspend operator fun <T1> invoke(function: KSuspendFunction1<T1, Unit>, arg: T1)
}

@Component
class RunBackgroundImpl(private val databaseClient: DatabaseClient) : RunBackground {
    private val objectMapper = jacksonObjectMapper()

    override suspend operator fun invoke(function: KSuspendFunction0<Unit>) {
        val serializedArgs = objectMapper.writeValueAsString(emptyList<Nothing>())
        databaseClient.sql("insert into job(class_fqn, method_name, args, status) values (?, ?, ?, ?)")
            .bind(0, function.javaMethod!!.declaringClass.canonicalName)
            .bind(1, function.name)
            .bind(2, serializedArgs)
            .bind(3, JobStatus.TODO)
            .await()
    }

    override suspend operator fun <T1> invoke(function: KSuspendFunction1<T1, Unit>, arg: T1) {
        val serializedArgs = objectMapper.writeValueAsString(listOf(arg))
        databaseClient.sql("insert into job(class_fqn, method_name, args, status) values (?, ?, ?, ?)")
            .bind(0, function.javaMethod!!.declaringClass.canonicalName)
            .bind(1, function.name)
            .bind(2, serializedArgs)
            .bind(3, JobStatus.TODO)
            .await()
    }
}
