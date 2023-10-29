package com.datingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class DatingAppTelegramBot {
    public static void main(String[] args) throws TelegramApiException {
        SpringApplication.run(DatingAppTelegramBot.class, args);
    }
}
