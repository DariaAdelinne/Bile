package com.sd.laborator

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.run(Application::class.java, *args)
    }

    @Controller
    class IntersectionController {
        @Post
        fun execute(@Body request: IntersectionRequest): IntersectionResponse {
            return handler.apply(request)
        }

        companion object {
            private val handler = IntersectionFunction()
        }
    }
}
