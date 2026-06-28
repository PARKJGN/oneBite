package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OneBiteApplication

fun main(args: Array<String>) {
    runApplication<OneBiteApplication>(*args)
}
