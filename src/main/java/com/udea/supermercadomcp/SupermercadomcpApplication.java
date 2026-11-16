package com.udea.supermercadomcp;

import com.udea.supermercadomcp.service.SupermercadoToolsService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SupermercadomcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupermercadomcpApplication.class, args);
	}

	/** Publica como herramientas MCP los métodos anotados con @Tool. */
	@Bean
	public ToolCallbackProvider supermercadoTools(SupermercadoToolsService tools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(tools)
				.build();
	}
}

