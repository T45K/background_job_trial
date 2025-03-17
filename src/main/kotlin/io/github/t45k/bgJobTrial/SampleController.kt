package io.github.t45k.bgJobTrial

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SampleController(private val sampleUseCase: SampleUseCase) {
    @GetMapping("/sample")
    suspend fun sample(): String {
        sampleUseCase.call()
        return "DONE"
    }
}

@Component
class SampleUseCase(
    private val sampleService: SampleService,
    private val runBackground: RunBackground,
) {
    suspend fun call() {
        runBackground(sampleService::call, SampleId(1))
    }
}

@Service
class SampleService {
    suspend fun call(id: SampleId) {
    }
}

@JvmInline
value class SampleId(val value: Long)
