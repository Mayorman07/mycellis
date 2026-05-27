package com.mycelis;

import com.mycelis.config.MonitoringProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MonitoringProperties.class)
public class MonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitorApplication.class, args);
	}

}
