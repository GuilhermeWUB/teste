package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.UserRegistrationDto;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    public void testLogin_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("pageTitle"));
    }

    @Test
    public void testRegisterGet_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    public void testRegisterPost_WithValidData_ShouldRegisterAndRedirect() throws Exception {
        UserAccount newUser = new UserAccount();
        newUser.setId(1L);
        newUser.setUsername("newuser");

        when(userAccountService.existsByUsername("newuser")).thenReturn(false);
        when(userAccountService.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userAccountService.register(any(UserRegistrationDto.class))).thenReturn(newUser);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("fullName", "New User")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userAccountService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    public void testRegisterPost_WithMismatchedPasswords_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("fullName", "New User")
                        .param("password", "password123")
                        .param("confirmPassword", "differentpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(userAccountService, never()).register(any());
    }

    @Test
    public void testRegisterPost_WithExistingUsername_ShouldReturnError() throws Exception {
        when(userAccountService.existsByUsername("existinguser")).thenReturn(true);

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("email", "newuser@example.com")
                        .param("fullName", "New User")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(userAccountService, never()).register(any());
    }

    @Test
    public void testRegisterPost_WithExistingEmail_ShouldReturnError() throws Exception {
        when(userAccountService.existsByUsername("newuser")).thenReturn(false);
        when(userAccountService.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "existing@example.com")
                        .param("fullName", "New User")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(userAccountService, never()).register(any());
    }

    @Test
    public void testLogoutView_ShouldReturnLogoutView() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("logout"));
    }

    @Test
    public void testLogoutSuccess_ShouldReturnLogoutSuccessView() throws Exception {
        mockMvc.perform(get("/logout/success"))
                .andExpect(status().isOk())
                .andExpect(view().name("logout_success"));
    }
}
