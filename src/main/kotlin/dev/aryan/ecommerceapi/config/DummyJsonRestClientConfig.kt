package dev.aryan.ecommerceapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class DummyJsonRestClientConfig {

    @Bean
    fun dummyJsonRestClient(builder: RestClient.Builder): RestClient =
        builder.baseUrl("https://dummyjson.com").build()
}
