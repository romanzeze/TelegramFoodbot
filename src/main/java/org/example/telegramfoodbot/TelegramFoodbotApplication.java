package org.example.telegramfoodbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TelegramFoodbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramFoodbotApplication.class, args);
    }
}
