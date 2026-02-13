package com.financal.mgt.Financal.Management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FinancalManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancalManagementApplication.class, args);
	}

}
