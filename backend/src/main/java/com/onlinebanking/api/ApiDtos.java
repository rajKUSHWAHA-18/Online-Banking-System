package com.onlinebanking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.onlinebanking.account.AccountType;
import com.onlinebanking.transaction.LedgerEntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 90) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72) String password,
            @NotNull AccountType accountType) { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) { }

    public record CreateAccountRequest(
            @NotNull AccountType type,
            @NotBlank @Size(max = 40) String name) { }

    public record DepositRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description) { }

    public record TransferRequest(
            @NotBlank @Size(min = 12, max = 12) String fromAccountNumber,
            @NotBlank @Size(min = 12, max = 12) String toAccountNumber,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description) { }

    public record CustomerView(UUID id, String fullName, String email, Instant createdAt) { }

    public record AccountView(UUID id, String accountNumber, String name, AccountType type,
                              BigDecimal balance, String currency, Instant createdAt) { }

    public record AccountListView(List<AccountView> accounts, BigDecimal totalBalance, String currency) { }

    public record TransactionView(UUID id, UUID referenceId, String accountNumber, LedgerEntryType type,
                                  BigDecimal amount, BigDecimal balanceAfter, String description,
                                  String counterpartyAccountNumber, Instant createdAt) { }

    public record MessageView(String message) { }

    public record ErrorView(String error, String message, Instant timestamp) { }
}