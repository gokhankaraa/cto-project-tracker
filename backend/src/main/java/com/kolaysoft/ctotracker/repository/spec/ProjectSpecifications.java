package com.kolaysoft.ctotracker.repository.spec;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.dto.filter.DashboardFilter;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;

/**
 * Proje sorgusu için filtre koşulları (filtre sözleşmesi T13, bölüm 2 ve 7).
 *
 * <p>Her metot tek bir koşul üretir; değer verilmemişse koşul kısıtlama yapmaz. T14'te rol bazlı
 * görünürlük koşulu (proje yöneticisi yalnızca kendi projeleri) de buraya eklenecek.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    /** Dashboard filtresindeki proje koşullarının (proje, durum, sorumlu) VE ile birleşimi. */
    public static Specification<Project> matching(DashboardFilter filter) {
        return Specification.allOf(
                hasId(filter.projectId()),
                statusIn(filter.projectStatuses()),
                ownedBy(filter.ownerId()));
    }

    public static Specification<Project> hasId(Long projectId) {
        if (projectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("id"), projectId);
    }

    public static Specification<Project> statusIn(Collection<ProjectStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /** Projenin sorumlu proje yöneticisi. */
    public static Specification<Project> ownedBy(Long ownerId) {
        if (ownerId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }
}
