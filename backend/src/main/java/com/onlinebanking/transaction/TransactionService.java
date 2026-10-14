package com.onlinebanking.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.onlinebanking.account.AccountRepository;
import com.onlinebanking.account.AccountService;
import com.onlinebanking.account.BankAccount;
import com.onlinebanking.api.ApiDtos;
import com.onlinebanking.api.ApiException;
import com.onlinebanking.customer.Customer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999.99");
    private final AccountRepository accounts;
    private final LedgerEntryRepository ledgerEntries;

    public TransactionService(AccountRepository accounts, LedgerEntryRepository ledgerEntries) {
        this.accounts = accounts;
        this.ledgerEntries = ledgerEntries;
    }

    @Transactional
    public List<ApiDtos.TransactionView> transfer(Customer customer, ApiDtos.TransferRequest request) {
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose two different accounts.");
        }

        List<String> accountNumbers = List.of(request.fromAccountNumber(), request.toAccountNumber())
                .stream().sorted(Comparator.naturalOrder()).toList();
        BankAccount first = lockAccount(accountNumbers.get(0));
        BankAccount second = lockAccount(accountNumbers.get(1));
        BankAccount source = first.getAccountNumber().equals(request.fromAccountNumber()) ? first : second;
        BankAccount destination = first.getAccountNumber().equals(request.toAccountNumber()) ? first : second;

        if (!source.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Source account not found.");
        }
        if (!source.isActive() || !destination.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Both accounts must be active.");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Insufficient funds for this transfer.");
        }
        if (destination.getBalance().add(request.amount()).compareTo(MAX_BALANCE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This transfer exceeds the destination balance limit.");
        }

        String description = AccountService.normalizeDescription(request.description(), "Account transfer");
        UUID referenceId = UUID.randomUUID();
        source.withdraw(request.amount());
        destination.deposit(request.amount());
        LedgerEntry outgoing = new LedgerEntry(referenceId, source, LedgerEntryType.TRANSFER_OUT,
                request.amount(), description, destination.getAccountNumber());
        LedgerEntry incoming = new LedgerEntry(referenceId, destination, LedgerEntryType.TRANSFER_IN,
                request.amount(), description, source.getAccountNumber());
        ledgerEntries.saveAll(List.of(outgoing, incoming));
        return List.of(toView(outgoing), toView(incoming));
    }

    private BankAccount lockAccount(String accountNumber) {
        return accounts.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private ApiDtos.TransactionView toView(LedgerEntry entry) {
        return new ApiDtos.TransactionView(entry.getId(), entry.getReferenceId(),
                entry.getAccount().getAccountNumber(), entry.getType(), normalize(entry.getAmount()),
                normalize(entry.getBalanceAfter()), entry.getDescription(), entry.getCounterpartyAccountNumber(),
                entry.getCreatedAt());
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}