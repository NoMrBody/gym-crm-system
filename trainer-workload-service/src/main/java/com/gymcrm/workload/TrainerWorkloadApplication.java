package com.gymcrm.workload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class TrainerWorkloadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainerWorkloadApplication.class, args);
    }
}
