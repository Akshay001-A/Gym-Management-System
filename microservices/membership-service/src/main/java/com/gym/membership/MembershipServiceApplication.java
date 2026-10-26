
package com.gym.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MembershipServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MembershipServiceApplication.class, args);
	}
}
