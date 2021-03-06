package com.example.demo.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.jwt.JwtUtils;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.enums.ERole;
import com.example.demo.payload.request.LoginRequest;
import com.example.demo.payload.request.SignupRequest;
import com.example.demo.payload.response.JwtResponse;
import com.example.demo.payload.response.MessageResponse;
import com.example.demo.repos.RoleRepo;
import com.example.demo.repos.UserRepo;
import com.example.demo.service.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	UserRepo userRepo;
	
	@Autowired
	RoleRepo roleRepo;
	
	@Autowired
	PasswordEncoder encoder;
	
	@Autowired
	JwtUtils jwtUtils;
	
	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest lr) {
		Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(lr.getUsername(), lr.getPassword()));
		
		SecurityContextHolder.getContext().setAuthentication(auth);
		String jwt = jwtUtils.generateJwtToken(auth);
		
		UserDetailsImpl udets = (UserDetailsImpl) auth.getPrincipal();
		List<String> roles = udets.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList());
		
		return ResponseEntity.ok(new JwtResponse(jwt, udets.getId(), udets.getUsername(), udets.getEmail(), roles));
	}
	
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest sr) {
		if (userRepo.existsByUsername(sr.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Username already taken.."));
		}
		if (userRepo.existsByEmail(sr.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Email already exists in our logs.."));
		}
		
		User user = new User(sr.getUsername(), sr.getEmail(), encoder.encode(sr.getPassword()));
		Set<String> strRoles = sr.getRole();
		Set<Role> roles = new HashSet<>();
		
		if (strRoles == null) {
			Role ur = roleRepo.findByName(ERole.USER).orElseThrow(() -> new RuntimeException("Error"));
			roles.add(ur);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepo.findByName(
							ERole.ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(adminRole);
					break;
				case "mod":
					Role modRole = roleRepo.findByName(
							ERole.MOD)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(modRole);
					break;
				default:
					Role userRole = roleRepo.findByName(
							ERole.USER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(userRole);
				}
			});
		}
		
		user.setRoles(roles);
		userRepo.save(user);
		
		return ResponseEntity.ok(new MessageResponse("User registered"));
	}
}
