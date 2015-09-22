package io.authomator.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{


	public SecurityConfig(){
		super(true);
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception{
		http.anonymous();			
		http.exceptionHandling();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.authorizeRequests()
			.antMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
			.antMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
			.antMatchers(HttpMethod.POST, "/api/auth/refresh/*").permitAll()
			.antMatchers(HttpMethod.POST, "/api/auth/forgot/*").permitAll()
			.antMatchers(HttpMethod.POST, "/api/auth/change").permitAll()
			.anyRequest().denyAll();
	}
	
}
