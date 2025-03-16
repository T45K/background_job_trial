package io.github.t45k.bgJobTrial

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BackgroundJobTrialApplication

fun main(args: Array<String>) {
    runApplication<BackgroundJobTrialApplication>(*args)
}
