package dev.elsboo.resilience4jhandson

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Resilience4jHandsonApplication

fun main(args: Array<String>) {
    runApplication<Resilience4jHandsonApplication>(*args)
}
