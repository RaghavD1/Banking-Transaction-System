
package com.newgen.banking.service;

import com.newgen.banking.entity.Account;
import com.newgen.banking.entity.Customer;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {

        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    public Account createAccount(Long customerId, String accountNumber) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("Account number already exists");
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(BigDecimal.ZERO);

        return accountRepository.save(account);
    }

    public Account getAccount(String accountNumber) {

        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new RuntimeException("Account not found"));
    }

    public BigDecimal getBalance(String accountNumber) {

        Account account = getAccount(accountNumber);

        return account.getBalance();
    }
}
