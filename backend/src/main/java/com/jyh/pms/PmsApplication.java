package com.jyh.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 小型人员管理系统（后台）启动类。
 *
 * <p>技术栈：Spring Boot 3 + Spring Security(JWT) + MongoDB + Redis。
 * 通过 {@code pms.storage} / {@code pms.cache} / {@code pms.token-store} 开关，
 * 本地开发可用纯内存实现零依赖启动，生产切换为 MongoDB + Redis。</p>
 *
 * <p>flapdoodle 内嵌 MongoDB 的开关放在 {@code application.yml} 的
 * {@code spring.autoconfigure.exclude} 里（配置驱动，不写死在注解上）：
 * 默认排除它，dev profile 全程内存存储、不需要任何 Mongo 实例；
 * mongodb / prod profile 把该列表覆盖为空即启用内嵌实现。
 * 这样 dev 下不会去解析 {@code de.flapdoodle.mongodb.embedded.version}，
 * 也不会尝试下载 mongod 二进制。</p>
 */
@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
public class PmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmsApplication.class, args);
    }
}
