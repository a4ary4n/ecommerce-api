package dev.aryan.ecommerceapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * Provides the [RestClient] ingestion uses to call dummyjson. Requires the
 * `spring-boot-starter-restclient` dependency for the auto-configured
 * [RestClient.Builder] - Boot 4.1 moved `RestClient` auto-configuration into that
 * separate module; `spring-boot-starter-webmvc` alone doesn't pull it in.
 */
@Configuration
class DummyJsonRestClientConfig {

    @Bean
    fun dummyJsonRestClient(builder: RestClient.Builder): RestClient =
        builder.baseUrl("https://dummyjson.com").build()
}
