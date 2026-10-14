package com.onlinebanking.api;

import java.util.Map;

import com.onlinebanking.config.SessionContextStore;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CustomerService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerService customers;
    private final AuthenticationManager authenticationManager;
    private final SessionContextStore sessionContextStore;
    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(CustomerService customers, AuthenticationManager authenticationManager, SessionContextStore sessionContextStore) {
        this.customers = customers;
        this.authenticationManager = authenticationManager;
        this.sessionContextStore = sessionContextStore;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiDtos.CustomerView> register(@Valid @RequestBody ApiDtos.RegisterRequest request,
                                                          HttpServletRequest servletRequest,
                                                          HttpServletResponse servletResponse) {
        Customer customer = customers.register(request);
        authenticate(customer.getEmail(), request.password(), servletRequest, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(customer));
    }

    @PostMapping("/login")
    public ApiDtos.CustomerView login(@Valid @RequestBody ApiDtos.LoginRequest request,
                                      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        saveAuthentication(authentication, servletRequest, servletResponse);
        return toView(customers.getByEmail(authentication.getName()));
    }

    @GetMapping("/me")
    public ApiDtos.CustomerView me(Authentication authentication) {
        return toView(customers.getByEmail(authentication.getName()));
    }

    @PostMapping("/logout")
    public ApiDtos.MessageView logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionContextStore.clear(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new ApiDtos.MessageView("Signed out successfully.");
    }

    private void authenticate(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        saveAuthentication(authentication, request, response);
    }

    private void saveAuthentication(Authentication authentication, HttpServletRequest request,
                                    HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        if (request.getSession(false) != null) {
            request.changeSessionId();
            session = request.getSession(true);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        sessionContextStore.save(session.getId(), context);
        response.addCookie(new Cookie("JSESSIONID", session.getId()));
    }

    private ApiDtos.CustomerView toView(Customer customer) {
        return new ApiDtos.CustomerView(customer.getId(), customer.getFullName(),
                customer.getEmail(), customer.getCreatedAt());
    }
}