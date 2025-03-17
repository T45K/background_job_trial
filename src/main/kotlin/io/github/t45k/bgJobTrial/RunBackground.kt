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
        val serializedJob = objectMapper.writeValueAsString(
            Job(
                function.javaMethod!!.declaringClass.canonicalName,
                function.name,
                emptyList()
            )
        )
        databaseClient.sql("insert into job(serialized_job, status) values (?, ?)")
            .bind(0, serializedJob)
            .bind(1, JobStatus.TODO)
            .await()
    }

    override suspend operator fun <T1> invoke(function: KSuspendFunction1<T1, Unit>, arg: T1) {
        val serializedJob = objectMapper.writeValueAsString(
            Job(
                function.javaMethod!!.declaringClass.canonicalName,
                function.name,
                listOf(arg)
            )
        )
        databaseClient.sql("insert into job(serialized_job, status) values (?, ?)")
            .bind(0, serializedJob)
            .bind(1, JobStatus.TODO)
            .await()
    }
}
