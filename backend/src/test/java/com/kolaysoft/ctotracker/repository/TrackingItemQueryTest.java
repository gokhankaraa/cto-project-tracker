package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.dto.filter.TrackingItemFilter;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.spec.TrackingItemSpecifications;

/**
 * Risk / engel / karar / aksiyon filtre kosullarinin gercek veritabaninda testi (filtre sozlesmesi, bolum 4a).
 * "Bugun" 2026-11-10 kabul edilir; kayitlarin tarihleri buna goredir.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class TrackingItemQueryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TrackingItemRepository repository;

    private Project p1;
    private User owner2;

    @BeforeEach
    void setUp() {
        User owner1 = user("p1@example.com");
        owner2 = user("p2@example.com");
        p1 = project("P1", owner1);
        Project p2 = project("P2", owner2);

        item(p1, TrackingItemType.RISK, "acik risk", RiskLevel.YUKSEK, "Ayse Yilmaz", TODAY.plusDays(3),
                TrackingItemStatus.ACIK);
        item(p1, TrackingItemType.AKSIYON, "gecikmis aksiyon", null, "ayse_k", TODAY.minusDays(1),
                TrackingItemStatus.DEVAM_EDIYOR);
        item(p1, TrackingItemType.AKSIYON, "bugun takipli", null, "%100 Ekip", TODAY, TrackingItemStatus.ACIK);
        // Kapali, takip tarihi gecmis: gecikmis sayilmaz. Kapali ve tarihsiz karar: "gecikmemis" listesinde olmali.
        item(p1, TrackingItemType.ENGEL, "kapali engel", RiskLevel.ORTA, "Mehmet", TODAY.minusDays(5),
                TrackingItemStatus.KAPANDI);
        item(p1, TrackingItemType.KARAR, "verilmis karar", null, null, null, TrackingItemStatus.KAPANDI);
        item(p2, TrackingItemType.KARAR, "p2 karari", null, "Zeynep", TODAY.plusDays(1), TrackingItemStatus.ACIK);
        em.flush();
        em.clear();
    }

    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(email);
        u.setRole(Role.PROJECT_MANAGER);
        return em.persist(u);
    }

    private Project project(String name, User owner) {
        Project p = new Project();
        p.setName(name);
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        p.setOwner(owner);
        return em.persist(p);
    }

    private void item(Project project, TrackingItemType type, String title, RiskLevel level, String assignee,
                      LocalDate followUp, TrackingItemStatus status) {
        TrackingItem item = new TrackingItem();
        item.setProject(project);
        item.setType(type);
        item.setTitle(title);
        item.setRiskLevel(level);
        item.setAssignee(assignee);
        item.setFollowUpDate(followUp);
        item.setStatus(status);
        item.setClosedDate(status.isOpen() ? null : TODAY.minusDays(1));
        item.setCreatedAt(Instant.parse("2026-11-01T08:00:00Z"));
        em.persist(item);
    }

    private static TrackingItemFilter filter(Long projectId, List<TrackingItemType> types,
                                             List<TrackingItemStatus> statuses, List<RiskLevel> levels,
                                             Boolean overdue, String assignee) {
        return new TrackingItemFilter(projectId, types, statuses, levels, overdue, assignee);
    }

    private List<String> titles(TrackingItemFilter filter) {
        return repository.findAll(TrackingItemSpecifications.matching(filter, TODAY), Sort.by("title")).stream()
                .map(TrackingItem::getTitle)
                .toList();
    }

    @Test
    @DisplayName("Gecikme: yalnizca kapanmamis ve takip tarihi bugunden once olan; bugun takipli gecikmis degil")
    void overdue() {
        assertThat(titles(filter(null, null, null, null, true, null))).containsExactly("gecikmis aksiyon");
    }

    @Test
    @DisplayName("Gecikmemis: kapali kayitlar (tarihsiz karar dahil) ve vadesi gelmemisler; NULL tarih elenmez")
    void notOverdueKeepsRecordsWithoutDate() {
        assertThat(titles(filter(null, null, null, null, false, null))).containsExactly(
                "acik risk", "bugun takipli", "kapali engel", "p2 karari", "verilmis karar");
    }

    @Test
    @DisplayName("Cok degerli tur, durum ve risk seviyesi kendi icinde VEYA, birbirleriyle VE")
    void multiValueFilters() {
        assertThat(titles(filter(null, List.of(TrackingItemType.RISK, TrackingItemType.ENGEL), null, null, null, null)))
                .containsExactly("acik risk", "kapali engel");
        assertThat(titles(filter(null, null, List.of(TrackingItemStatus.ACIK, TrackingItemStatus.DEVAM_EDIYOR),
                null, null, null)))
                .containsExactly("acik risk", "bugun takipli", "gecikmis aksiyon", "p2 karari");
        assertThat(titles(filter(null, List.of(TrackingItemType.KARAR), List.of(TrackingItemStatus.ACIK),
                null, null, null)))
                .containsExactly("p2 karari");
        assertThat(titles(filter(null, null, null, List.of(RiskLevel.ORTA, RiskLevel.YUKSEK), null, null)))
                .containsExactly("acik risk", "kapali engel");
    }

    @Test
    @DisplayName("Sorumlu aramasi buyuk/kucuk harfe duyarsiz 'icerir'; % ve _ joker degil duz karakter")
    void assigneeSearch() {
        assertThat(titles(filter(null, null, null, null, null, "AYSE"))).containsExactly("acik risk", "gecikmis aksiyon");
        assertThat(titles(filter(null, null, null, null, null, "e_k"))).containsExactly("gecikmis aksiyon");
        assertThat(titles(filter(null, null, null, null, null, "%100"))).containsExactly("bugun takipli");
    }

    @Test
    @DisplayName("Proje ve proje sahibi kosullari")
    void projectAndOwner() {
        assertThat(titles(filter(p1.getId(), List.of(TrackingItemType.KARAR), null, null, null, null)))
                .containsExactly("verilmis karar");
        assertThat(repository.findAll(TrackingItemSpecifications.projectOwnedBy(owner2.getId())))
                .extracting(TrackingItem::getTitle).containsExactly("p2 karari");
    }

    @Test
    @DisplayName("Dashboard sorgusu: verilen projelerin kapanmamis kayitlari")
    void openItemsOfProjects() {
        assertThat(repository.findByProjectIdInAndStatusNot(List.of(p1.getId()), TrackingItemStatus.KAPANDI))
                .extracting(TrackingItem::getTitle)
                .containsExactlyInAnyOrder("acik risk", "gecikmis aksiyon", "bugun takipli");
    }
}
