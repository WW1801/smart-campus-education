package com.campus.education.config;

/**
 * 安全配置类，负责定义系统认证与授权规则。
 */

import com.campus.education.common.JwtUtils;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.mapper.UserMapper;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    // 创建密码编码器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 配置安全访问规则
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
                .antMatchers(HttpMethod.POST, "/department", "/major", "/class").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/department", "/major", "/class").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/department/**", "/major/**", "/class/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/semester", "/classroom").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.PUT, "/semester", "/classroom").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.DELETE, "/semester/**", "/classroom/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/schedule-basic", "/schedule-basic/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.PUT, "/schedule-basic", "/schedule-basic/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.DELETE, "/schedule-basic/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/student").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/student/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/student/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.GET, "/student/list", "/student/page").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/teacher").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/teacher").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/teacher/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/course").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/course").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/course/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/teaching-plan").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/teaching-plan").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.DELETE, "/teaching-plan/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/schedule", "/schedule/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.PUT, "/schedule", "/schedule/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.DELETE, "/schedule/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.POST, "/grade", "/grade/**").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.PUT, "/grade/*/approve").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/grade/*/reject").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.GET, "/grade/statistics").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/attendance", "/attendance/**").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.PUT, "/attendance", "/attendance/**").hasAnyRole("1", "2", "3", "4")
                .antMatchers(HttpMethod.DELETE, "/attendance/**").hasAnyRole("1", "2")
                .antMatchers(HttpMethod.GET, "/attendance/statistics").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/leave-requests").hasRole("5")
                .antMatchers(HttpMethod.GET, "/leave-requests/my").hasRole("5")
                .antMatchers(HttpMethod.PUT, "/leave-requests/*/cancel").hasRole("5")
                .antMatchers(HttpMethod.GET, "/leave-requests/page").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/leave-requests/batch-approve").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/leave-requests/*/approve", "/leave-requests/*/reject").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/graduation/audit/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.POST, "/graduation/batch-audit").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.PUT, "/graduation/degree/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.GET, "/graduation/**").hasAnyRole("1", "2", "3")
                .antMatchers(HttpMethod.GET, "/agent/**").hasRole("1")
                .antMatchers(HttpMethod.POST, "/agent/**").hasRole("1")
                .antMatchers(HttpMethod.PUT, "/agent/**").hasRole("1")
                .antMatchers("/selection/select").hasAnyRole("5")
                .antMatchers("/selection/*/drop").hasAnyRole("5")
                .antMatchers("/selection/my-courses").hasAnyRole("5")
                .antMatchers("/selection/my-schedule").hasAnyRole("5")
                .antMatchers("/selection/available").hasAnyRole("5")
                .antMatchers("/selection/page").hasAnyRole("1", "2")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, userMapper, studentMapper, teacherMapper), UsernamePasswordAuthenticationFilter.class);
    }

    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtUtils jwtUtils;
        private final UserMapper userMapper;
        private final StudentMapper studentMapper;
        private final TeacherMapper teacherMapper;
        private final ObjectMapper objectMapper = new ObjectMapper();

        // 初始化 JWT 认证过滤器
        JwtAuthenticationFilter(JwtUtils jwtUtils, UserMapper userMapper, StudentMapper studentMapper, TeacherMapper teacherMapper) {
            this.jwtUtils = jwtUtils;
            this.userMapper = userMapper;
            this.studentMapper = studentMapper;
            this.teacherMapper = teacherMapper;
        }

        // 执行内部过滤处理
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

                    if (!isRelatedPersonAvailable(userId, roleId)) {
                        writeForbidden(response, "关联人员已停用、退学、离职或不存在，当前账号不能继续办理业务。请联系教务管理员。");
                        return;
                    }

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
            result.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        }

        private boolean isRelatedPersonAvailable(String userId, String roleId) {
            if (!"4".equals(roleId) && !"5".equals(roleId)) return true;
            User user = userMapper.selectById(userId);
            if (user == null) return false;
            if ("4".equals(roleId)) {
                Teacher teacher = teacherMapper.selectById(user.getRelatedId());
                return teacher != null && "active".equals(teacher.getStatus());
            }
            Student student = studentMapper.selectById(user.getRelatedId());
            return student != null && "active".equals(student.getStatus());
        }

        private void writeForbidden(HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>(); result.put("code", 403); result.put("message", message);
            result.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        }
    }
}
