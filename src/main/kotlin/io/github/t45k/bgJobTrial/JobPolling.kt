package io.github.t45k.bgJobTrial

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CancellationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Component
class JobPolling(
    private val transactionalOperator: TransactionalOperator = TransactionalOperator { },
) {
    private val objectMapper = jacksonObjectMapper()

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    suspend fun pollAndExecute() {
        transactionalOperator.execute {
            // select job record for update.
            // update job record status to IN_PROGRESS.
        }
        try {
            transactionalOperator.execute {
                // execute in transaction
                // update job record status to COMPLETED
            }
        } catch (e: Exception) {
            when (e) {
                // When app receives SIGTERM, it seems to stop scheduled job by throwing JobCancellationException of coroutine.
                // update job record status to TODO to try again in other pods.
                is CancellationException -> {
                    JobStatus.TODO
                    println("interrupted")
                }

                // For other errors, update job record status to FAILED.
                // The job should prompt users to try again by itself.
                else -> {
                    JobStatus.FAILED
                }
            }
        }
    }
}

// tmp
fun interface TransactionalOperator {
    fun execute(suspend: () -> Unit)
}

@Service
class XXXService {
    suspend fun exec(id: XXXId) {
    }
}

@JvmInline
value class XXXId(val value: Long)
