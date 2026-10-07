package com.hrutvik.orders.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  private final ApiKeyAuthenticator authenticator;
  public ApiKeyAuthenticationFilter(ApiKeyAuthenticator authenticator){this.authenticator=authenticator;}
  @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith("/v1/");}
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
    var identity=authenticator.authenticate(request.getHeader("X-Api-Key"));
    if(identity.isEmpty()){response.setStatus(401);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid API key\"}");return;}
    var authorities=identity.get().scopes().stream().map(scope->new SimpleGrantedAuthority("SCOPE_"+scope)).toList();
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(identity.get().merchantId(),null,authorities));
    chain.doFilter(request,response);
  }
}
