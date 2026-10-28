package com.gym.trainer.config;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret){
        byte[] keyBytes;try{keyBytes=Base64.getDecoder().decode(secret);}catch(IllegalArgumentException ex){throw new IllegalArgumentException("JWT_SECRET must be a valid Base64-encoded secret",ex);}
        if(keyBytes.length<32)throw new IllegalArgumentException("JWT_SECRET must contain at least 32 decoded bytes");
        SecretKey key=new SecretKeySpec(keyBytes,"HmacSHA256");return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,ObjectMapper mapper)throws Exception{
        JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("role");roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwt=new JwtAuthenticationConverter();jwt.setJwtGrantedAuthoritiesConverter(roles);
        http.csrf(csrf->csrf.disable()).formLogin(form->form.disable()).httpBasic(basic->basic.disable())
                .authorizeHttpRequests(auth->auth.requestMatchers("/actuator/health","/actuator/health/**","/actuator/info").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(oauth2->oauth2.jwt(config->config.jwtAuthenticationConverter(jwt)))
                .exceptionHandling(errors->errors.authenticationEntryPoint((req,res,ex)->writeError(req,res,mapper,HttpStatus.UNAUTHORIZED,"Authentication required"))
                        .accessDeniedHandler((req,res,ex)->writeError(req,res,mapper,HttpStatus.FORBIDDEN,"You do not have permission to access this resource")));
        return http.build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,ObjectMapper mapper,HttpStatus status,String message)throws IOException{
        response.setStatus(status.value());response.setContentType("application/json");Map<String,Object> body=new LinkedHashMap<>();body.put("timestamp",Instant.now().toString());body.put("status",status.value());body.put("code",status.name());body.put("error",status.getReasonPhrase());body.put("message",message);body.put("path",request.getRequestURI());mapper.writeValue(response.getOutputStream(),body);
    }
}