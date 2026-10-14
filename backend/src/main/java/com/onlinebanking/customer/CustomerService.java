package com.onlinebanking.customer;

import java.util.Locale;

import com.onlinebanking.account.AccountService;
import com.onlinebanking.account.AccountType;
import com.onlinebanking.api.ApiDtos;
import com.onlinebanking.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accounts;

    public CustomerService(CustomerRepository customers, PasswordEncoder passwordEncoder, AccountService accounts) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.accounts = accounts;
    }

    @Transactional
    public Customer register(ApiDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (customers.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        Customer customer = customers.save(new Customer(
                request.fullName().trim(), email, passwordEncoder.encode(request.password())));
        accounts.createAccount(customer, request.accountType(), "Everyday account");
        return customer;
    }

    @Transactional(readOnly = true)
    public Customer getByEmail(String email) {
        return customers.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found."));
    }
}