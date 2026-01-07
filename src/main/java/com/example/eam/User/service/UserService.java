package com.example.eam.User.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;


    //create user
        public Users register(UserCreateDto dto) {
            // Destructure the DTO (Java-style)
            String firstName = dto.getFirstName();
            String lastName = dto.getLastName();
            String password = dto.getPassword();
            String email = dto.getEmail().trim().toLowerCase();

            // Check if the user already exists
            Optional<Users> existingUser = usersRepository.findByEmailAndDeletedFalse(email);
            if (existingUser.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with this email");
            }

            // Assign Admin role to the new user
            Role ownerRole = roleRepository.findByNameIgnoreCase("Admin")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));

            // Create the user object and assign the role
            Users newUser = new Users();
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setDeleted(false);
            newUser.setPassword(passwordEncoder.encode(password));
            

            // Save the user
            Users savedUser = usersRepository.save(newUser);  // This should now correctly insert into the user_roles join table


            // Create the UserRole object to link the user with the role
            UserRole userRole = UserRole.builder()
                    .user(savedUser)
                    .role(ownerRole)
                    .build();

            // Save the UserRole to establish the relationship between the user and the role
            userRoleRepository.save(userRole);

            return savedUser;
        }

}
