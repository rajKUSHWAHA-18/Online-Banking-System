package com.onlinebanking.customer;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService implements UserDetailsService {

    private final CustomerRepository customers;

    public CustomerDetailsService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Customer customer = customers.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found."));
        return User.withUsername(customer.getEmail())
                .password(customer.getPasswordHash())
                .roles("CUSTOMER")
                .build();
    }
}