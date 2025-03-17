package io.github.t45k.bgJobTrial

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BackgroundJobTrialApplication(private val databaseClient: DatabaseClient) {
    @PostConstruct
    fun initTable() {
        databaseClient.sql("drop table if exists job").then().block()
        databaseClient.sql(
            """
                create table job(
                    serialized_job text not null,
                    status enum('TODO', 'IN_PROGRESS', 'COMPLETED', 'FAILED') not null
                )
            """.trimIndent()
        ).then().block()
    }
}

fun main(args: Array<String>) {
    runApplication<BackgroundJobTrialApplication>(*args)
}
