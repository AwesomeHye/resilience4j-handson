package dev.elsboo.resilience4jhandson.client

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.test.Test

class PostsCircuitClientTest {
    private val log = LoggerFactory.getLogger(javaClass)
    private val postsClientMock = mock(PostsClient::class.java)


    /*
        1. 실패율 50% -> 몇 초 동안 들어온 호출에서 50%가 넘어야 하는지 궁금했는데 시간이아니라 슬라이딩윈도우 베이스였음
        2. API 3번 모두 호출 실패했는데 OPEN 안 바뀜 -> 실패율 계산 위한 최소 호출수를 넘겨야함 그게 minimumNumberOfCalls 이고 디폴트 100임
        3. 시간 기다리면 HALF_OPEN 로 전환되는지 알았음 -> 그게 아니라 API 호출이 상태 변환 트리거임
        4. HALF_OPEN 상태에서 API 호출이 실패하면 다시 OPEN 으로 전환됨
     */
    @Test
    fun `half-open 테스트`() {
        // given
        `when`(postsClientMock.getPosts()).thenThrow(RuntimeException::class.java)

        val SLIDING_WINDOW_SIZE = 5
        val WAIT_DURATION_IN_OPEN_STATE_SECOND = 1L

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_IN_OPEN_STATE_SECOND))
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)


        // when
        for (i in 1..SLIDING_WINDOW_SIZE) {
            postsCircuitClient.getPosts()
        }

        // then
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("posts")

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)

        Thread.sleep(Duration.ofSeconds(WAIT_DURATION_IN_OPEN_STATE_SECOND).toMillis()) // HALF_OPEN 으로 바뀔 때 까지 최소 시간 대기
        postsCircuitClient.getPosts() // 상태 변환 트리거

        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.state)
    }

    @Test
    fun `failureRateThreshold 를 넘지 않으면 CLOSE 다`() {
        // given
        // 3번은 성공하고 2번은 실패
        `when`(postsClientMock.getPosts())
            .thenThrow(RuntimeException::class.java)
            .thenReturn(listOf("Post 1"))
            .thenReturn(listOf("Post 2"))
            .thenThrow(RuntimeException::class.java)
            .thenReturn(listOf("Post 3"))

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

        // when
        for(i in 1..5) {
            postsCircuitClient.getPosts()
        }

        // then
        circuitBreakerRegistry.circuitBreaker("posts").let {
            // let: null 아닐 때만 실행, 반환값 있어도 됨, 기본적으로 $it 로 접근
            assertEquals(CircuitBreaker.State.CLOSED, it.state)
        }

    }

    @Test
    fun `failureRateThreshold 를 넘으면 OPEN 이다`() {
        // given
        // 2번은 성공하고 3번은 실패
        `when`(postsClientMock.getPosts())
            .thenThrow(RuntimeException::class.java)
            .thenReturn(listOf("Post 1"))
            .thenReturn(listOf("Post 2"))
            .thenThrow(RuntimeException::class.java)
            .thenReturn(listOf("Post 3"))

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

        // when
        for(i in 1..5) {
            postsCircuitClient.getPosts()
        }

        // then
        circuitBreakerRegistry.circuitBreaker("posts").let {
            assertEquals(CircuitBreaker.State.CLOSED, it.state)
        }

    }

    @Test
    fun `서킷 열리면 CallNotPermittedException 를 반환한다`() {
        // given
        `when`(postsClientMock.getPosts()).thenThrow(RuntimeException::class.java)

        val SLIDING_WINDOW_SIZE = 2
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

        // when
        var response = emptyList<String>()
        for(i in 1 .. SLIDING_WINDOW_SIZE + 1) {
            response = postsCircuitClient.getPosts()
        }

        // then
        // CallNotPermittedException 에 대한 fallback 문구 반환
        assertEquals(response.get(0), "Blocked by circuit breaker.")
    }

    @Test
    fun `minimumNumberOfCalls 를 넘어야 OPEN 될 수 있다`() {
        // given
        `when`(postsClientMock.getPosts()).thenThrow(RuntimeException::class.java)

        val MINIMUM_NUMBER_OF_CALLS = 3
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .slidingWindowSize(MINIMUM_NUMBER_OF_CALLS)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

        // when
        for (i in 1..MINIMUM_NUMBER_OF_CALLS) {
            postsCircuitClient.getPosts()
        }

        // then
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("posts")
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }
// methodSource 로 묶기, 블로그 올리기
    @Test
    fun `minimumNumberOfCalls 를 넘지않으면 서킷이 열리지 않는다`() {
        // given
        `when`(postsClientMock.getPosts()).thenThrow(RuntimeException::class.java)

        val MINIMUM_NUMBER_OF_CALLS = 3
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .slidingWindowSize(MINIMUM_NUMBER_OF_CALLS)
                .build()
        )

        val postsCircuitClient = PostsCircuitClient(postsClientMock, circuitBreakerRegistry)

        // when
        for (i in 1..MINIMUM_NUMBER_OF_CALLS - 1) {
            postsCircuitClient.getPosts()
        }

        // then
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("posts")
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

}


