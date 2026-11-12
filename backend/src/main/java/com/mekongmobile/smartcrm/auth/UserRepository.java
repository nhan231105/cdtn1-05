package com.mekongmobile.smartcrm.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<UserAccount> findEnabled(String username) {
        return jdbc.query("SELECT username, password_hash, role, center_id FROM app_user WHERE username = ? AND enabled = TRUE",
                (rs, row) -> new UserAccount(new UserIdentity(rs.getString("username"), rs.getString("role"),
                        rs.getObject("center_id", Long.class)), rs.getString("password_hash")), username).stream().findFirst();
    }

    public List<Long> centerIds() {
        return jdbc.queryForList("SELECT center_id FROM service_center", Long.class);
    }

    public void createIfAbsent(String username, String role, Long centerId, String hash) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Long.class, username) == 0) {
            jdbc.update("INSERT INTO app_user (username, password_hash, role, center_id) VALUES (?, ?, ?, ?)",
                    username, hash, role, centerId);
        }
    }

    public record UserAccount(UserIdentity identity, String passwordHash) { }
}
