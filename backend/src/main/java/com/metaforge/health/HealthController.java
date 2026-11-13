package com.metaforge.health;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metaforge.common.BizException;
import com.metaforge.common.ErrorCode;
import com.metaforge.common.Result;

/** 健康检查：确认后端在运行，并且能连上数据库。前端首页和环境验收都用它。 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public Result<Health> health() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String database = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
            return Result.ok(new Health("UP", database, LocalDateTime.now().format(TIME_FORMAT)));
        } catch (SQLException e) {
            throw new BizException(ErrorCode.INTERNAL, "连不上数据库：" + e.getMessage());
        }
    }

    public record Health(String status, String database, String time) {
    }
}
