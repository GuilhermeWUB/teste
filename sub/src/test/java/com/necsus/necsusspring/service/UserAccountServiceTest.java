package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.UserRegistrationDto;
import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserAccountServiceTest {

    @InjectMocks
    private UserAccountService userAccountService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserAccount testUser;
    private UserRegistrationDto testRegistrationDto;

    @BeforeEach
    public void setUp() {
        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("encodedPassword");
        testUser.setRole(RoleType.USER.getCode());

        testRegistrationDto = new UserRegistrationDto();
        testRegistrationDto.setUsername("newuser");
        testRegistrationDto.setEmail("newuser@example.com");
        testRegistrationDto.setFullName("New User");
        testRegistrationDto.setPassword("password123");
    }

    @Test
    public void testRegister_ShouldCreateUserWithEncodedPassword() {
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        UserAccount result = userAccountService.register(testRegistrationDto);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("New User", result.getFullName());
        assertEquals("encodedPassword123", result.getPassword());
        assertEquals(RoleType.USER.getCode(), result.getRole());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userAccountRepository, times(1)).save(any(UserAccount.class));
    }

    @Test
    public void testExistsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        when(userAccountRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = userAccountService.existsByUsername("testuser");

        assertTrue(result);
        verify(userAccountRepository, times(1)).existsByUsername("testuser");
    }

    @Test
    public void testExistsByUsername_WhenUsernameDoesNotExist_ShouldReturnFalse() {
        when(userAccountRepository.existsByUsername("nonexistent")).thenReturn(false);

        boolean result = userAccountService.existsByUsername("nonexistent");

        assertFalse(result);
        verify(userAccountRepository, times(1)).existsByUsername("nonexistent");
    }

    @Test
    public void testExistsByUsername_WithNullUsername_ShouldReturnFalse() {
        boolean result = userAccountService.existsByUsername(null);

        assertFalse(result);
        verify(userAccountRepository, never()).existsByUsername(any());
    }

    @Test
    public void testExistsByEmail_WhenEmailExists_ShouldReturnTrue() {
        when(userAccountRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = userAccountService.existsByEmail("test@example.com");

        assertTrue(result);
        verify(userAccountRepository, times(1)).existsByEmail("test@example.com");
    }

    @Test
    public void testExistsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        when(userAccountRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        boolean result = userAccountService.existsByEmail("nonexistent@example.com");

        assertFalse(result);
        verify(userAccountRepository, times(1)).existsByEmail("nonexistent@example.com");
    }

    @Test
    public void testExistsByEmail_WithNullEmail_ShouldReturnFalse() {
        boolean result = userAccountService.existsByEmail(null);

        assertFalse(result);
        verify(userAccountRepository, never()).existsByEmail(any());
    }

    @Test
    public void testFindAll_ShouldReturnSortedUsers() {
        List<UserAccount> users = Arrays.asList(testUser);
        when(userAccountRepository.findAll(any(Sort.class))).thenReturn(users);

        List<UserAccount> result = userAccountService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userAccountRepository, times(1)).findAll(Sort.by(Sort.Direction.ASC, "fullName"));
    }

    @Test
    public void testFindByUsername_WhenUserExists_ShouldReturnUser() {
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<UserAccount> result = userAccountService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userAccountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    public void testFindByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<UserAccount> result = userAccountService.findByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(userAccountRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    public void testFindByUsername_WithNullUsername_ShouldReturnEmpty() {
        Optional<UserAccount> result = userAccountService.findByUsername(null);

        assertFalse(result.isPresent());
        verify(userAccountRepository, never()).findByUsername(any());
    }

    @Test
    public void testFindByEmail_WhenUserExists_ShouldReturnUser() {
        when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<UserAccount> result = userAccountService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userAccountRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    public void testFindByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
        when(userAccountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<UserAccount> result = userAccountService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(userAccountRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    public void testFindByEmail_WithNullEmail_ShouldReturnEmpty() {
        Optional<UserAccount> result = userAccountService.findByEmail(null);

        assertFalse(result.isPresent());
        verify(userAccountRepository, never()).findByEmail(any());
    }

    @Test
    public void testUpdateRole_WithValidRole_ShouldUpdateUserRole() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);
        when(userAccountRepository.countByRoleIn(any(Set.class))).thenReturn(2L);

        userAccountService.updateRole(1L, "ADMIN");

        assertEquals(RoleType.ADMIN.getCode(), testUser.getRole());
        verify(userAccountRepository, times(1)).findById(1L);
        verify(userAccountRepository, times(1)).save(testUser);
    }

    @Test
    public void testUpdateRole_WithInvalidRole_ShouldThrowException() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userAccountService.updateRole(1L, "INVALID_ROLE");
        });

        assertTrue(exception.getMessage().contains("Tipo de permissão inválido"));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    public void testUpdateRole_WhenUserNotFound_ShouldThrowException() {
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userAccountService.updateRole(999L, "USER");
        });

        assertTrue(exception.getMessage().contains("Usuário não encontrado"));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    public void testUpdateRole_RemovingLastAdmin_ShouldThrowException() {
        testUser.setRole(RoleType.ADMIN.getCode());
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountRepository.countByRoleIn(any(Set.class))).thenReturn(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userAccountService.updateRole(1L, "USER");
        });

        assertTrue(exception.getMessage().contains("último usuário com acesso administrativo"));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    public void testUpdateRole_RemovingAdminWhenMultipleExist_ShouldSucceed() {
        testUser.setRole(RoleType.ADMIN.getCode());
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountRepository.countByRoleIn(any(Set.class))).thenReturn(2L);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);

        userAccountService.updateRole(1L, "USER");

        assertEquals(RoleType.USER.getCode(), testUser.getRole());
        verify(userAccountRepository, times(1)).save(testUser);
    }

    @Test
    public void testLoadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails result = userAccountService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        verify(userAccountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    public void testLoadUserByUsername_WhenUserDoesNotExist_ShouldThrowException() {
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userAccountService.loadUserByUsername("nonexistent");
        });

        assertTrue(exception.getMessage().contains("Usuário não encontrado"));
        verify(userAccountRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    public void testLoadUserByUsername_WithAdminRole_ShouldReturnAdminAuthority() {
        testUser.setRole(RoleType.ADMIN.getCode());
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails result = userAccountService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }
}
