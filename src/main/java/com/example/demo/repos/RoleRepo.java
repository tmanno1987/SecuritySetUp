package com.example.demo.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Role;
import com.example.demo.model.enums.ERole;

public interface RoleRepo extends JpaRepository<Role, Long> {
	Optional<Role> findByName(ERole name);
}
