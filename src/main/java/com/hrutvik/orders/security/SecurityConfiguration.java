package com.hrutvik.orders.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
  @Bean SecurityFilterChain security(HttpSecurity http,ApiKeyAuthenticationFilter apiKeys)throws Exception{
    return http.csrf(csrf->csrf.disable()).sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(errors->errors
      .authenticationEntryPoint((request,response,exception)->{response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");})
      .accessDeniedHandler((request,response,exception)->{response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"API key lacks the required scope\"}");})).authorizeHttpRequests(auth->auth
      .requestMatchers("/actuator/health","/actuator/health/**","/actuator/prometheus","/docs/**","/v3/api-docs/**").permitAll()
      .requestMatchers(HttpMethod.POST,"/v1/orders").hasAuthority("SCOPE_ORDERS_WRITE")
      .requestMatchers(HttpMethod.GET,"/v1/orders/**").hasAuthority("SCOPE_ORDERS_READ")
      .anyRequest().authenticated()).addFilterBefore(apiKeys,UsernamePasswordAuthenticationFilter.class).build();
  }
}
