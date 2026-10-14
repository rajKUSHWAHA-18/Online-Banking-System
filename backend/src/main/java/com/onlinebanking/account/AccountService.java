package com.onlinebanking.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import com.onlinebanking.api.ApiDtos;
import com.onlinebanking.api.ApiException;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.transaction.LedgerEntry;
import com.onlinebanking.transaction.LedgerEntryRepository;
import com.onlinebanking.transaction.LedgerEntryType;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999.99");
    private final AccountRepository accounts;
    private final LedgerEntryRepository ledgerEntries;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accounts, LedgerEntryRepository ledgerEntries) {
        this.accounts = accounts;
        this.ledgerEntries = ledgerEntries;
    }

    @Transactional(readOnly = true)
    public ApiDtos.AccountListView listAccounts(Customer customer) {
        List<ApiDtos.AccountView> accountViews = accounts.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(AccountService::toView).toList();
        BigDecimal total = accountViews.stream().map(ApiDtos.AccountView::balance)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new ApiDtos.AccountListView(accountViews, normalize(total), "INR");
    }

    @Transactional
    public ApiDtos.AccountView createAccount(Customer customer, AccountType type, String name) {
        BankAccount account = createAccount(customer, type, name, true);
        return toView(account);
    }

    public BankAccount createAccount(Customer customer, AccountType type, String name, boolean persist) {
        BankAccount account = new BankAccount(generateAccountNumber(), customer, type, name);
        return persist ? accounts.save(account) : account;
    }

    @Transactional
    public ApiDtos.AccountView deposit(Customer customer, String accountNumber, ApiDtos.DepositRequest request) {
        BankAccount account = lockOwnedAccount(customer, accountNumber);
        ensureCanAccept(account, request.amount());
        account.deposit(request.amount());
        ledgerEntries.save(new LedgerEntry(UUID.randomUUID(), account, LedgerEntryType.DEPOSIT,
                request.amount(), normalizeDescription(request.description(), "Cash deposit"), null));
        return toView(account);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.TransactionView> listTransactions(Customer customer, int limit) {
        int boundedLimit = Math.clamp(limit, 1, 100);
        return ledgerEntries.findAllByAccountCustomerIdOrderByCreatedAtDesc(customer.getId(),
                        PageRequest.of(0, boundedLimit))
                .stream()
                .map(entry -> new ApiDtos.TransactionView(entry.getId(), entry.getReferenceId(),
                        entry.getAccount().getAccountNumber(), entry.getType(), normalize(entry.getAmount()),
                        normalize(entry.getBalanceAfter()), entry.getDescription(), entry.getCounterpartyAccountNumber(),
                        entry.getCreatedAt()))
                .toList();
    }

    private BankAccount lockOwnedAccount(Customer customer, String accountNumber) {
        BankAccount account = accounts.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found.");
        }
        if (!account.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "This account is not active.");
        }
        return account;
    }

    private void ensureCanAccept(BankAccount account, BigDecimal amount) {
        if (account.getBalance().add(amount).compareTo(MAX_BALANCE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This deposit exceeds the account balance limit.");
        }
    }

    public static String normalizeDescription(String description, String fallback) {
        return description == null || description.isBlank() ? fallback : description.trim();
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "61" + String.format("%010d", secureRandom.nextLong(0, 10_000_000_000L));
        } while (accounts.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    static ApiDtos.AccountView toView(BankAccount account) {
        BigDecimal normalized = normalize(account.getBalance());
        return new ApiDtos.AccountView(account.getId(), account.getAccountNumber(), account.getName(),
                account.getType(), normalized, account.getCurrency(), account.getCreatedAt());
    }

    static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}