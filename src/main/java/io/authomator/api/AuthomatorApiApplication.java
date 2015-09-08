package io.authomator.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan({"io.authomator.api"})
@EnableMongoRepositories("io.authomator.api.domain.repository")
public class AuthomatorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthomatorApiApplication.class, args);
    }
}
