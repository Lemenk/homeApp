package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.security.UserContext;
import com.familyhome.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/ledgers/{ledgerId}/statistics/trend")
    public Result<List<Map<String, Object>>> trend(
        @PathVariable Long ledgerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "month") String groupBy) {
        return Result.ok(statisticsService.trend(UserContext.require(), ledgerId, startDate, endDate, groupBy));
    }

    @GetMapping("/ledgers/{ledgerId}/statistics/category")
    public Result<List<Map<String, Object>>> category(
        @PathVariable Long ledgerId,
        @RequestParam String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(statisticsService.category(UserContext.require(), ledgerId, type, startDate, endDate));
    }
}
