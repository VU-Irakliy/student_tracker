package com.studio.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Entry point for the Studio Student Management backend.
 *
 * <p>Manages students, their recurring class schedules, one-off sessions,
 * per-class and package-based payment tracking, and a calendar view.
 */
@SpringBootApplication
@EnableScheduling
public class StudentMgmtApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentMgmtApplication.class, args);
    }

    @Bean
    public Clock appClock() {
        return Clock.systemUTC();
    }
}
