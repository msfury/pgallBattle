package com.pgall.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PgallBattleApplication {
    public static void main(String[] args) {
        SpringApplication.run(PgallBattleApplication.class, args);
    }
}
