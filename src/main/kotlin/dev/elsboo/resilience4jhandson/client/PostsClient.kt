package dev.elsboo.resilience4jhandson.client

import org.springframework.stereotype.Component


@Component
class PostsClient {

    public fun getPosts(): List<String> {
        // Simulate a call to an external service
        return listOf("Post 1", "Post 2", "Post 3")
    }
}
