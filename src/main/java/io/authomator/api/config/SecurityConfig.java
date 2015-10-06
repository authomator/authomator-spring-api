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
			.antMatchers(HttpMethod.POST, "/sign-in").permitAll()
			.antMatchers(HttpMethod.POST, "/register").permitAll()
			.antMatchers(HttpMethod.POST, "/refresh-tokens").permitAll()
			.antMatchers(HttpMethod.POST, "/forgot-password").permitAll()
			.antMatchers(HttpMethod.POST, "/reset-password").permitAll()
			.antMatchers(HttpMethod.PUT,  "/password").permitAll()
			.antMatchers(HttpMethod.POST, "/send-confirm-email").permitAll()
			.antMatchers(HttpMethod.POST, "/confirm-email").permitAll()			
			.anyRequest().denyAll();
	}
	
}
