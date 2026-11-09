package com.kolaysoft.ctotracker.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.dto.UserResponse;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;

/** Kullanici listesi (salt okunur): sorumlu secimi ve sorumlu filtresi icin. */
@Service
@Transactional(readOnly = true)
public class UserService {

    /** Ada gore, ayni adlilarda id'ye gore: liste her istekte ayni sirada gelir. */
    private static final Sort BY_NAME = Sort.by("fullName", "id");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @param role verilirse yalnizca bu roldeki kullanicilar */
    public List<UserResponse> findAll(Role role) {
        List<User> users = role == null
                ? userRepository.findAll(BY_NAME)
                : userRepository.findByRole(role, BY_NAME);
        return users.stream().map(UserService::toResponse).toList();
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getRole());
    }
}
