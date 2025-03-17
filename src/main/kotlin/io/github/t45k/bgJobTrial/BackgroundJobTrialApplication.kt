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
                    id bigint auto_increment not null primary key,
                    serialization_key varchar(256) not null,
                    class_fqn varchar(256) not null,
                    method_name varchar(256) not null,
                    args text not null,
                    status enum('TODO', 'IN_PROGRESS', 'COMPLETED', 'FAILED') not null,
                    created_at datetime not null default current_timestamp,
                    index job(status)
                )
            """.trimIndent()
        ).then().block()
    }
}

fun main(args: Array<String>) {
    runApplication<BackgroundJobTrialApplication>(*args)
}
