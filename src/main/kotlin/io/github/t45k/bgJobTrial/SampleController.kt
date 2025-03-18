package io.github.t45k.bgJobTrial

import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

@RestController
class SampleController(
    private val runBackground: RunBackground,
    private val sampleService: SampleService,
) {
    private val counter = AtomicLong()

    @GetMapping("/complete")
    suspend fun complete(): String {
        runBackground(JobType.COMPLETE, sampleService::complete, SampleId(counter.incrementAndGet()), SampleValue(counter.incrementAndGet().toString()))
        return "DONE"
    }

    @GetMapping("/interrupted")
    suspend fun interrupted(): String {
        runBackground(JobType.INTERRUPTED, sampleService::interrupted)
        return "DONE"
    }

    @GetMapping("/exception")
    suspend fun exception(): String {
        runBackground(JobType.EXCEPTION, sampleService::exception)
        return "DONE"
    }
}

@Service
class SampleService {
    suspend fun complete(id: SampleId, value: SampleValue) {
        delay(3_000)
    }

    suspend fun interrupted() {
        delay(Long.MAX_VALUE)
    }

    suspend fun exception() {
        throw RuntimeException("test")
    }
}

data class SampleId(val value: Long)

@JvmInline
value class SampleValue(val value: String)
