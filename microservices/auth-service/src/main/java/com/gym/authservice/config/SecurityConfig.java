package com.gym.authservice.config;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,ObjectMapper mapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth.requestMatchers("/api/auth/register","/api/auth/login","/actuator/health","/actuator/health/**","/actuator/info","/api/admin-setup/**").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(oauth2->oauth2.jwt(Customizer.withDefaults()))
                .exceptionHandling(errors->errors.authenticationEntryPoint((req,res,ex)->writeError(req,res,mapper,HttpStatus.UNAUTHORIZED,"Authentication required"))
                        .accessDeniedHandler((req,res,ex)->writeError(req,res,mapper,HttpStatus.FORBIDDEN,"You do not have permission to access this resource")));
        return http.build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,ObjectMapper mapper,HttpStatus status,String message)throws IOException{
        response.setStatus(status.value());response.setContentType("application/json");Map<String,Object> body=new LinkedHashMap<>();body.put("timestamp",Instant.now().toString());body.put("status",status.value());body.put("code",status.name());body.put("error",status.getReasonPhrase());body.put("message",message);body.put("path",request.getRequestURI());mapper.writeValue(response.getOutputStream(),body);
    }
}