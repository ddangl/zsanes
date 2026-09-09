package com.anes.schedule.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger 文档元信息;UI:/api/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI anesOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("麻醉科排班+考勤系统 API")
                .description("认证方式:POST /auth/login 获取 JWT 后,请求头附带 Authorization: Bearer <token>")
                .version("v0.1"));
    }
}
