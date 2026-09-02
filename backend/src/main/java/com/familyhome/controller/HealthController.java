package com.familyhome.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 健康检查接口：用于探活/负载均衡健康检查 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /** 返回服务健康状态（{status: ok}） */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
