package com.kolaysoft.ctotracker.security;

import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.Role;

/**
 * Istegi yapan kullanici ve gorebilecegi projeler (yetki matrisi, README "Permissions").
 *
 * <ul>
 *   <li>Admin ve CTO tum projeleri gorur.</li>
 *   <li>Proje yoneticisi yalnizca sorumlusu oldugu projeleri ve onlarin rapor / is kalemlerini gorur.
 *       Baskasinin kaydina id ile gitmek 404 doner: kaydin var oldugu bile disari sizmaz.</li>
 * </ul>
 *
 * Kimin ne yazabilecegi (rol) controller'lardaki {@code @PreAuthorize} ile, neyi gorebilecegi (sahiplik)
 * burada belirlenir; yazma islemleri de once kaydi "gorebilmeyi" ister.
 *
 * @param userId kullanicinin id'si; kimligi token disindan gelen (test) bir oturumda null olabilir
 */
public record Viewer(Long userId, Role role) {

    /** Hicbir kullanici id'si bu degeri almaz; id'si bilinmeyen proje yoneticisi hicbir proje gormez. */
    private static final long NO_USER = -1L;

    public boolean seesAllProjects() {
        return role != Role.PROJECT_MANAGER;
    }

    /**
     * Sorgulara eklenecek sorumlu kosulu: tum projeleri gorenler icin null (kisit yok), proje yoneticisi
     * icin kendi id'si. Mevcut "sorumlu" filtreleriyle ({@code ownedBy}) ayni kosul oldugu icin filtre kodu
     * degismez; ikisi VE ile birlesir (baskasinin ownerId'si ile filtreleyen PM bos sonuc alir).
     */
    public Long visibleOwnerId() {
        if (seesAllProjects()) {
            return null;
        }
        return userId != null ? userId : NO_USER;
    }

    public boolean canSee(Project project) {
        if (seesAllProjects()) {
            return true;
        }
        return project.getOwner() != null && project.getOwner().getId().equals(userId);
    }
}
