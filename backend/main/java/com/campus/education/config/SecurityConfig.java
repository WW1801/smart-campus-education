package com.campus.education.config;

import com.campus.education.common.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtUtils jwtUtils;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .httpBasic().disable()
            .formLogin().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/login").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/system/**").hasAnyRole("1")
                .antMatchers(HttpMethod.POST, "/student").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/student/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/student/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/teacher").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/teacher").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/teacher/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/course").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/course").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/course/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/teaching-plan").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/teaching-plan").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/teaching-plan/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/schedule").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.PUT, "/schedule").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.DELETE, "/schedule/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/grade").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.PUT, "/grade/*/approve").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/grade/*/reject").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/attendance").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.PUT, "/attendance").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.DELETE, "/attendance/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/graduation/audit/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/graduation/batch-audit").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/graduation/degree/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.GET, "/graduation/**").hasAnyRole("1", "2", "3")
                .antMatchers("/selection/select").hasAnyRole("5")
                .antMatchers("/selection/*/drop").hasAnyRole("5")
                .antMatchers("/selection/my-courses").hasAnyRole("5")
                .antMatchers("/selection/my-schedule").hasAnyRole("5")
                .antMatchers("/selection/available").hasAnyRole("5")
                .antMatchers("/selection/page").hasAnyRole("1", "2")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
    }

    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtUtils jwtUtils;
        private final ObjectMapper objectMapper = new ObjectMapper();

        JwtAuthenticationFilter(JwtUtils jwtUtils) {
            this.jwtUtils = jwtUtils;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String servletPath = request.getServletPath();
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            boolean isLoginRequest = "/login".equals(servletPath)
                    || "/login".equals(requestUri)
                    || (contextPath != null && !contextPath.isEmpty() && (contextPath + "/login").equals(requestUri));

            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (jwtUtils.validateToken(token)) {
                    String userId = jwtUtils.getUserIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);
                    String roleId = jwtUtils.getRoleIdFromToken(token);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(roleId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    filterChain.doFilter(request, response);
                    return;
                }
            }

            if (isLoginRequest || request.getMethod().equals("OPTIONS")) {
                filterChain.doFilter(request, response);
                return;
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "未登录或登录已过期");
            response.getWriter().write(objectMapper.writeValueAsString(result));
        }
    }
}
