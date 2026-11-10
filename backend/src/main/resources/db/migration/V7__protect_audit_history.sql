-- V7: degisiklik gecmisi veritabaninda da yalnizca eklenir. UPDATE, DELETE ve TRUNCATE trigger ile reddedilir; bir
-- uygulama hatasi ya da elle calistirilan yanlis bir SQL gecmisi degistiremez.
--
-- Tek istisna: kullanici silinince yabanci anahtar (ON DELETE SET NULL) olayin actor_id'sini bosaltir. Bu UPDATE'te
-- actor_id disinda hicbir kolon degisemez; yazan kisinin adi (actor_name) kalir.
--
-- Sinir: tablo sahibi trigger'i kapatabilir (ALTER TABLE ... DISABLE TRIGGER). Tam koruma icin uygulama, tablo sahibi
-- olmayan ve gecmis tablolarinda yalnizca INSERT/SELECT yetkisi olan bir veritabani roluyle calismalidir (README).

CREATE FUNCTION reject_audit_change() RETURNS trigger AS $$
BEGIN
    -- OLD/NEW yalnizca satir duzeyindeki UPDATE'te okunur (TRUNCATE'te tanimli degildir).
    IF TG_OP = 'UPDATE' AND TG_TABLE_NAME = 'audit_events' THEN
        IF OLD.actor_id IS NOT NULL AND NEW.actor_id IS NULL
                AND (NEW.id, NEW.occurred_at, NEW.actor_name, NEW.action, NEW.entity_type, NEW.entity_id,
                     NEW.project_id, NEW.summary)
                    IS NOT DISTINCT FROM
                    (OLD.id, OLD.occurred_at, OLD.actor_name, OLD.action, OLD.entity_type, OLD.entity_id,
                     OLD.project_id, OLD.summary) THEN
            RETURN NEW;
        END IF;
    END IF;
    RAISE EXCEPTION 'Degisiklik gecmisi degistirilemez ve silinemez (% %).', TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_audit_change();

CREATE TRIGGER trg_audit_events_no_truncate
    BEFORE TRUNCATE ON audit_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_change();

CREATE TRIGGER trg_audit_event_changes_append_only
    BEFORE UPDATE OR DELETE ON audit_event_changes
    FOR EACH ROW EXECUTE FUNCTION reject_audit_change();

CREATE TRIGGER trg_audit_event_changes_no_truncate
    BEFORE TRUNCATE ON audit_event_changes
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_change();
