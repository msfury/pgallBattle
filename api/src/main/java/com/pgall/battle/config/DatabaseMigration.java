package com.pgall.battle.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * SQLite는 ddl-auto: update가 새 컬럼 추가를 제대로 못하는 경우가 있어
 * 앱 시작 시 누락된 컬럼/테이블을 수동으로 추가한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            // equipment 테이블에 enhance_level 컬럼 추가
            addColumnIfNotExists(conn, "equipment", "enhance_level", "INTEGER DEFAULT 0");

            // inventory 테이블에 equipped 컬럼 추가
            addColumnIfNotExists(conn, "inventory", "equipped", "BOOLEAN DEFAULT 0");

            // enhance_effect 테이블 생성
            createEnhanceEffectTableIfNotExists(conn);

            log.info("Database migration completed successfully.");
        } catch (SQLException e) {
            log.error("Database migration failed: {}", e.getMessage(), e);
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String type) throws SQLException {
        if (!columnExists(conn, table, column)) {
            String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.info("Added column {}.{}", table, column);
            }
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    private void createEnhanceEffectTableIfNotExists(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS enhance_effect (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id BIGINT NOT NULL,
                    effect VARCHAR(50) NOT NULL,
                    effect_chance INTEGER DEFAULT 0,
                    effect_value INTEGER DEFAULT 0,
                    FOREIGN KEY (equipment_id) REFERENCES equipment(id)
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Ensured enhance_effect table exists.");
        }
    }
}
