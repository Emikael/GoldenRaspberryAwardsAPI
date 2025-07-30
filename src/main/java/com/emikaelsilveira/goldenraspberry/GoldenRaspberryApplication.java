package com.emikaelsilveira.goldenraspberry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class GoldenRaspberryApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldenRaspberryApplication.class, args);
    }
}
