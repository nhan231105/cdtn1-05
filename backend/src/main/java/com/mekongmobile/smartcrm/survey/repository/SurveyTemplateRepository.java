package com.mekongmobile.smartcrm.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class SurveyTemplateRepository {
    public record Template(long templateId, String name, String title, String introText,
                           boolean active, String createdAt, String updatedAt) {}

    private final JdbcTemplate jdbc;

    public SurveyTemplateRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Template> findAll() {
        return jdbc.query("SELECT template_id,name,title,intro_text,is_active,created_at,updated_at FROM survey_template ORDER BY template_id",
                (rs, row) -> map(rs));
    }

    public Optional<Template> findById(long id) {
        return jdbc.query("SELECT template_id,name,title,intro_text,is_active,created_at,updated_at FROM survey_template WHERE template_id=?",
                (rs, row) -> map(rs), id).stream().findFirst();
    }

    public Optional<Template> lockById(long id) {
        return jdbc.query("SELECT template_id,name,title,intro_text,is_active,created_at,updated_at FROM survey_template WHERE template_id=? FOR UPDATE",
                (rs, row) -> map(rs), id).stream().findFirst();
    }

    public Optional<Template> findActive() {
        return jdbc.query("SELECT template_id,name,title,intro_text,is_active,created_at,updated_at FROM survey_template WHERE is_active=TRUE",
                (rs, row) -> map(rs)).stream().findFirst();
    }

    public Optional<Template> lockActive() {
        return jdbc.query("SELECT template_id,name,title,intro_text,is_active,created_at,updated_at FROM survey_template WHERE is_active=TRUE FOR UPDATE",
                (rs, row) -> map(rs)).stream().findFirst();
    }

    public boolean nameExists(String name, Long exceptId) {
        Integer count = exceptId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM survey_template WHERE name=?", Integer.class, name)
                : jdbc.queryForObject("SELECT COUNT(*) FROM survey_template WHERE name=? AND template_id<>?", Integer.class, name, exceptId);
        return count != null && count > 0;
    }

    public long insert(String name, String title, String introText, boolean active) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO survey_template(name,title,intro_text,is_active) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, title);
            statement.setString(3, introText);
            statement.setBoolean(4, active);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public void deactivateAll() { jdbc.update("UPDATE survey_template SET is_active=FALSE WHERE is_active=TRUE"); }

    public void update(long id, String name, String title, String introText, boolean active) {
        jdbc.update("UPDATE survey_template SET name=?,title=?,intro_text=?,is_active=? WHERE template_id=?",
                name, title, introText, active, id);
    }

    public void delete(long id) { jdbc.update("DELETE FROM survey_template WHERE template_id=?", id); }

    private static Template map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Template(rs.getLong("template_id"), rs.getString("name"), rs.getString("title"),
                rs.getString("intro_text"), rs.getBoolean("is_active"), rs.getString("created_at"), rs.getString("updated_at"));
    }
}
