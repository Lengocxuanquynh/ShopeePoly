package com.fpoly.java5demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fpoly.java5demo.components.AuthInterceptor;

@Configuration
public class AuthConfig implements WebMvcConfigurer {
	@Autowired
	private AuthInterceptor interceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(interceptor)
				.addPathPatterns("/admin/**", "/seller/**", "/user/**", "/checkout/**");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:uploads/");
		registry.addResourceHandler("/static/**")
				.addResourceLocations("classpath:/static/");
	}
}
