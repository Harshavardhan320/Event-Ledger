package com.event.event_gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EventGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventGatewayServiceApplication.class, args);
	}

}
