package com.kolaysoft.ctotracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Verilen roldeki kullanicilar (sorumlu secimi icin). */
    List<User> findByRole(Role role, Sort sort);
}
