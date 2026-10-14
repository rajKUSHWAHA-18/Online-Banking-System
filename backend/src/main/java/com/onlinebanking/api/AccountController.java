package com.onlinebanking.api;

import java.util.List;

import com.onlinebanking.account.AccountService;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CustomerService;
import com.onlinebanking.transaction.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final CustomerService customers;
    private final AccountService accounts;
    private final TransactionService transactions;

    public AccountController(CustomerService customers, AccountService accounts, TransactionService transactions) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @GetMapping("/accounts")
    public ApiDtos.AccountListView accounts(Authentication authentication) {
        return accounts.listAccounts(currentCustomer(authentication));
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.AccountView createAccount(Authentication authentication,
                                              @Valid @RequestBody ApiDtos.CreateAccountRequest request) {
        return accounts.createAccount(currentCustomer(authentication), request.type(), request.name());
    }

    @PostMapping("/accounts/{accountNumber}/deposits")
    public ApiDtos.AccountView deposit(Authentication authentication, @PathVariable String accountNumber,
                                       @Valid @RequestBody ApiDtos.DepositRequest request) {
        return accounts.deposit(currentCustomer(authentication), accountNumber, request);
    }

    @GetMapping("/transactions")
    public List<ApiDtos.TransactionView> transactions(Authentication authentication,
                                                       @RequestParam(defaultValue = "50") int limit) {
        return accounts.listTransactions(currentCustomer(authentication), limit);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ApiDtos.TransactionView> transfer(Authentication authentication,
                                                   @Valid @RequestBody ApiDtos.TransferRequest request) {
        return transactions.transfer(currentCustomer(authentication), request);
    }

    private Customer currentCustomer(Authentication authentication) {
        return customers.getByEmail(authentication.getName());
    }
}