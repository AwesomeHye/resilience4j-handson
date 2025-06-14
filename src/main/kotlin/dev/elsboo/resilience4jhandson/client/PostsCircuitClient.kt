package dev.elsboo.resilience4jhandson.client

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.vavr.control.Try
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class PostsCircuitClient (
    private val postsClient: PostsClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
)
{
    private val log = LoggerFactory.getLogger(javaClass)

    fun getPosts(): List<String> {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("posts")
        val decorateSupplier: Supplier<List<String>> = CircuitBreaker.decorateSupplier(circuitBreaker) {
            postsClient.getPosts()
        }
        return Try.ofSupplier(decorateSupplier)
                .recover(CallNotPermittedException::class.java) {
                    listOf("Blocked by circuit breaker.") // 서킷이 오픈되면 CallNotPermittedException 에 걸린다.
                }
                .recover { throwable -> fallback(throwable) }
                .get()
    }

    private fun fallback(throwable: Throwable): List<String> {
        return emptyList()
    }
}
