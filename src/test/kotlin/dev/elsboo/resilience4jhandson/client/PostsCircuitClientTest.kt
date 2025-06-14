package dev.elsboo.resilience4jhandson.client

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class PostsCircuitClientTest {
    private val log = LoggerFactory.getLogger(javaClass)
    private val postsClientMock = mock(PostsClient::class.java)

    @Autowired
    lateinit var circuitBreakerRegistry: CircuitBreakerRegistry


        @Test
        fun `half-open 테스트`() {
            val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

            `when` (postsClientMock.getPosts()).thenThrow(RuntimeException("test exception"))

            for (i in 1..99) {
                postsCircuitClient.getPosts()
            }

            val circuitBreaker = circuitBreakerRegistry.circuitBreaker("posts")

            log.info(circuitBreaker.state.toString())

            Thread.sleep(1000)
            postsCircuitClient.getPosts() // 상태 변환 트리거

            for (i in 1..3) {
                log.info(circuitBreaker.state.toString())
            }

            assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.state)
        }
}


// 1. 실패율 50% -> 얼마 동안의 호출에서 50%가 넘어야 하는지 궁금했는데 시간이아니라 슬라이딩윈도우 베이스였음
// 2. API 3번 모두 호출 실패했는데 OPEN 안 바뀜 -> 실패율 계산 위한 최소 호출수를 넘겨야함 그게 minimumNumberOfCalls 이고 디폴트 100이라함
// 3. 시간 기다리면 HALF_OPEN 로 전환되는지 알았음 -> 그게 아니라 API 호출이 상태 변환 트리거임

// 99, 100 차이?
