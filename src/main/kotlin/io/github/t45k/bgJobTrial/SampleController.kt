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
        runBackground(sampleService::complete, SampleId(counter.incrementAndGet()))
        return "DONE"
    }

    @GetMapping("/interrupted")
    suspend fun interrupted(): String {
        runBackground(sampleService::interrupted)
        return "DONE"
    }

    @GetMapping("/exception")
    suspend fun exception(): String {
        runBackground(sampleService::exception)
        return "DONE"
    }
}

@Service
class SampleService {
    suspend fun complete(id: SampleId) {
    }

    suspend fun interrupted() {
        delay(Long.MAX_VALUE - 1)
    }

    suspend fun exception() {
        throw RuntimeException("test")
    }
}

data class SampleId(val value: Long)
