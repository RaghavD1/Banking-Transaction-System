
package com.newgen.banking.service;

import com.newgen.banking.entity.*;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BankTransaction deposit(
            String accountNumber,
            BigDecimal amount,
            String transactionId) {

        validateAmount(amount);
        validateTransactionId(transactionId);
        checkDuplicate(transactionId);

        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new RuntimeException("Account not found"));

        validateAccount(account);

        account.setBalance(account.getBalance().add(amount));

        return saveTransaction(
                account, amount, TransactionType.DEPOSIT, transactionId);
    }

    @Transactional
    public BankTransaction withdraw(
            String accountNumber,
            BigDecimal amount,
            String transactionId) {

        validateAmount(amount);
        validateTransactionId(transactionId);
        checkDuplicate(transactionId);

        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new RuntimeException("Account not found"));

        validateAccount(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        return saveTransaction(
                account, amount, TransactionType.WITHDRAWAL, transactionId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
    }

    private void checkDuplicate(String transactionId) {
        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException("Duplicate transaction ID");
        }
    }

    private void validateAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
    }

    private BankTransaction saveTransaction(
            Account account,
            BigDecimal amount,
            TransactionType type,
            String transactionId) {

        BankTransaction transaction = new BankTransaction();

        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTransactionId(transactionId);

        return transactionRepository.save(transaction);
    }
}
