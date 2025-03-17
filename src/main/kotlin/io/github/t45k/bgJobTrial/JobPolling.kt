package io.github.t45k.bgJobTrial

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions

@Component
class JobPolling(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator,
    private val applicationContext: ApplicationContext,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    suspend fun pollAndExecute() {
        val (internalJobId, job) = transactionalOperator.executeAndAwait {
            val jobRecord = databaseClient.sql("select * from job where status = ? order by id desc limit 1 for update")
                .bind(0, JobStatus.TODO)
                .fetch()
                .one()
                .awaitFirstOrNull()
                ?: return@executeAndAwait null
            val internalJobId = jobRecord["id"]!!
            databaseClient.sql("update job set status = ? where id = ?")
                .bind(0, JobStatus.IN_PROGRESS)
                .bind(1, internalJobId)
                .await()

            internalJobId to Job(
                jobRecord["class_fqn"]!! as String,
                jobRecord["method_name"]!! as String,
                objectMapper.readTree(jobRecord["args"]!!.toString())
            )
        } ?: return

        val clazz = Class.forName(job.classFqn)
        val instance = applicationContext.getBean(clazz)

        try {
            transactionalOperator.executeAndAwait {
                val function = clazz.kotlin.declaredFunctions.first { it.name == job.methodName }
                val paramClasses = function.parameters.drop(1) // remove object itself
                    .map { it.type.classifier!! as KClass<*> }

                val deserializedArgs = paramClasses.zip(job.serializedArgs).map { (paramClass, arg) ->
                    arg?.let { objectMapper.treeToValue(it, paramClass.java) }
                }
                function.callSuspend(instance, *deserializedArgs.toTypedArray())

                databaseClient.sql("update job set status = ? where id = ?")
                    .bind(0, JobStatus.COMPLETED)
                    .bind(1, internalJobId)
                    .await()
            }
            logger.info("Completed $job") // debug
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    databaseClient.sql("update job set status = ? where id = ?")
                        .bind(0, JobStatus.TODO)
                        .bind(1, internalJobId)
                        .await()
                    logger.info("Interrupted $job") // debug
                }

                is InvocationTargetException -> {
                    databaseClient.sql("update job set status = ? where id = ?")
                        .bind(0, JobStatus.FAILED)
                        .bind(1, internalJobId)
                        .await()
                    logger.error("Failed to execute $job during execution", e.cause)
                }

                else -> {
                    databaseClient.sql("update job set status = ? where id = ?")
                        .bind(0, JobStatus.FAILED)
                        .bind(1, internalJobId)
                        .await()
                    logger.error("Failed to execute $job outer execution", e)
                }
            }
        }
    }
}

private data class Job(
    val classFqn: String,
    val methodName: String,
    val serializedArgs: JsonNode,
)
