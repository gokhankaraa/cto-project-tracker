package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditChange;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.AuditEvent;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;

/**
 * V7: degisiklik gecmisi veritabaninda yalnizca eklenir. Dogrudan SQL ile UPDATE, DELETE ve TRUNCATE reddedilir;
 * tek istisna kullanici silinince bosalan actor_id. PostgreSQL hatadan sonra transaction'i kapattigi icin her test
 * yalnizca bir reddedilen komut dener.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class AuditHistoryProtectionTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private User actor;
    private Long eventId;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setEmail("mehmet@ornek.com");
        actor.setFullName("Mehmet Kaya");
        actor.setRole(Role.PROJECT_MANAGER);
        entityManager.persist(actor);

        AuditEvent event = new AuditEvent();
        event.setOccurredAt(Instant.parse("2026-11-10T09:00:00Z"));
        event.setActorId(actor.getId());
        event.setActorName("Mehmet Kaya");
        event.setAction(AuditAction.GUNCELLENDI);
        event.setEntityType(AuditEntityType.TAKIP_KAYDI);
        event.setEntityId(4L);
        event.setProjectId(2L);
        event.setSummary("Test ortamina erisim yok");
        event.getChanges().add(new AuditChange("riskLevel", "ORTA", "YUKSEK"));
        eventId = entityManager.persistAndFlush(event).getId();
        entityManager.clear();
    }

    private static void assertRejected(Runnable sql) {
        // Mesaj PostgreSQL'in hatasinda (kok neden); Spring onu DataAccessException ile sarar.
        assertThatThrownBy(sql::run).isInstanceOf(DataAccessException.class)
                .rootCause().hasMessageContaining("Degisiklik gecmisi degistirilemez");
    }

    @Test
    @DisplayName("Olayin bir kolonu guncellenemez")
    void updateRejected() {
        assertRejected(() -> jdbc.update("UPDATE audit_events SET summary = 'Baska' WHERE id = ?", eventId));
    }

    @Test
    @DisplayName("Olay silinemez")
    void deleteRejected() {
        assertRejected(() -> jdbc.update("DELETE FROM audit_events WHERE id = ?", eventId));
    }

    @Test
    @DisplayName("Degisen alan guncellenemez")
    void changeUpdateRejected() {
        assertRejected(() -> jdbc.update("UPDATE audit_event_changes SET new_value = 'DUSUK' WHERE audit_event_id = ?",
                eventId));
    }

    @Test
    @DisplayName("Degisen alan silinemez")
    void changeDeleteRejected() {
        assertRejected(() -> jdbc.update("DELETE FROM audit_event_changes WHERE audit_event_id = ?", eventId));
    }

    @Test
    @DisplayName("Tablo bosaltilamaz (TRUNCATE)")
    void truncateRejected() {
        assertRejected(() -> jdbc.execute("TRUNCATE audit_events CASCADE"));
    }

    @Test
    @DisplayName("actor_id bosaltilirken baska bir kolon da degisiyorsa reddedilir")
    void actorClearWithOtherChangeRejected() {
        assertRejected(() -> jdbc.update(
                "UPDATE audit_events SET actor_id = NULL, actor_name = 'Baska' WHERE id = ?", eventId));
    }

    @Test
    @DisplayName("Kullanici silinince olay kalir: actor_id bosalir, ad ve alanlar ayni")
    void userDeletionClearsOnlyTheActor() {
        jdbc.update("DELETE FROM users WHERE id = ?", actor.getId());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT actor_id, actor_name, summary FROM audit_events WHERE id = ?", eventId);
        assertThat(row.get("actor_id")).isNull();
        assertThat(row).containsEntry("actor_name", "Mehmet Kaya").containsEntry("summary", "Test ortamina erisim yok");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event_changes WHERE audit_event_id = ?",
                Integer.class, eventId)).isEqualTo(1);
    }
}
