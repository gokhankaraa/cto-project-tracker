package com.kolaysoft.ctotracker.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditChange;
import com.kolaysoft.ctotracker.entity.AuditEvent;
import com.kolaysoft.ctotracker.repository.AuditEventRepository;
import com.kolaysoft.ctotracker.security.Actor;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * Degisiklik gecmisini yazar. Yazma servisleri degisikligi kaydettikten hemen sonra cagirir.
 *
 * <p>Gecmis, degisiklikle AYNI transaction'da yazilir ({@link Propagation#MANDATORY}: transaction yoksa hata).
 * Boylece reddedilen (400, 409) ya da geri alinan bir degisiklik iz birakmaz; gecmis yazilamazsa degisiklik de
 * geri alinir, izsiz degisiklik olmaz. Yazan kisi her zaman oturumdan alinir, istekten degil.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public AuditService(AuditEventRepository auditEventRepository, CurrentUser currentUser, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** Yeni kayit: dolu alanlar ilk degerleriyle yazilir. */
    public void created(AuditSubject after) {
        List<AuditChange> changes = after.values().entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> new AuditChange(e.getKey(), null, e.getValue()))
                .toList();
        save(AuditAction.OLUSTURULDU, after, changes);
    }

    /** Guncelleme: yalnizca degisen alanlar yazilir; hicbir alan degismediyse olay acilmaz. */
    public void updated(AuditSubject before, AuditSubject after) {
        Map<String, String> old = before.values();
        List<AuditChange> changes = after.values().entrySet().stream()
                .filter(e -> !Objects.equals(old.get(e.getKey()), e.getValue()))
                .map(e -> new AuditChange(e.getKey(), old.get(e.getKey()), e.getValue()))
                .toList();
        if (!changes.isEmpty()) {
            save(AuditAction.GUNCELLENDI, after, changes);
        }
    }

    /** Silme: kaydin adi ozette kalir, alan yazilmaz. */
    public void deleted(AuditSubject before) {
        save(AuditAction.SILINDI, before, List.of());
    }

    private void save(AuditAction action, AuditSubject subject, List<AuditChange> changes) {
        Actor actor = currentUser.actor();
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(Instant.now(clock));
        event.setActorId(actor.id());
        event.setActorName(actor.name());
        event.setAction(action);
        event.setEntityType(subject.type());
        event.setEntityId(subject.id());
        event.setProjectId(subject.projectId());
        event.setSummary(subject.summary());
        event.getChanges().addAll(changes);
        auditEventRepository.save(event);
    }
}
