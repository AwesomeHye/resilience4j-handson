package dev.elsboo.resilience4jhandson.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CircuitBreakerConfiguration {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        return CircuitBreakerRegistry.of(circuitBreakerConfig())
    }

    private fun circuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f) // 실패 요청 수기 50% 넘으면 서킷 오픈 기간은?
            .waitDurationInOpenState(java.time.Duration.ofSeconds(1)) // 서킷이 이 시간동안 OPEN 유지한 후 HALF_OPEN 으로 전환
            .permittedNumberOfCallsInHalfOpenState(3) // 서킷이 open 된 후 half-open 에서 이 만큼 호출해보고 실패율보다 낮으면 close 된다
            .slidingWindowSize(5) // 최근 몇개의 호출로 실패율 계산할건지, 디폴트 100
            .build()
    }
}
