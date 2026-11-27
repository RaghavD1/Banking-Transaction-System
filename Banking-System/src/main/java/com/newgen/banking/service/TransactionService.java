
package com.newgen.banking.service;

import com.newgen.banking.entity.*;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.TransactionRepository;
import com.newgen.banking.repository.TransactionAuditRepository;

import com.newgen.banking.exception.AccountNotFoundException;
import com.newgen.banking.exception.AccountInactiveException;
import com.newgen.banking.exception.DuplicateTransactionException;
import com.newgen.banking.exception.InsufficientBalanceException;
import com.newgen.banking.exception.InvalidTransactionException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionAuditRepository auditRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionAuditRepository auditRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
    }

    // DEPOSIT
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
                        new AccountNotFoundException("Account not found"));

        validateAccount(account);

        account.setBalance(account.getBalance().add(amount));

        return saveTransaction(
                account, amount, TransactionType.DEPOSIT, transactionId);
    }

    // WITHDRAW
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
                        new AccountNotFoundException("Account not found"));

        validateAccount(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        return saveTransaction(
                account, amount, TransactionType.WITHDRAWAL, transactionId);
    }

    // TRANSFER
    @Transactional
    public BankTransaction transfer(
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            String transactionId) {

        validateAmount(amount);
        validateTransactionId(transactionId);
        checkDuplicate(transactionId);

        if (fromAccountNumber == null || toAccountNumber == null ||
                fromAccountNumber.isBlank() || toAccountNumber.isBlank()) {
            throw new InvalidTransactionException(
                    "Source and destination account numbers are required");
        }

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidTransactionException(
                    "Source and destination accounts cannot be the same");
        }

        // Lock accounts in a consistent order to reduce deadlocks
        String firstAccount = fromAccountNumber.compareTo(toAccountNumber) < 0
                ? fromAccountNumber : toAccountNumber;

        String secondAccount = fromAccountNumber.compareTo(toAccountNumber) < 0
                ? toAccountNumber : fromAccountNumber;

        Account first = accountRepository
                .findByAccountNumberForUpdate(firstAccount)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found"));

        Account second = accountRepository
                .findByAccountNumberForUpdate(secondAccount)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found"));

        Account source = fromAccountNumber.equals(firstAccount)
                ? first : second;

        Account destination = toAccountNumber.equals(firstAccount)
                ? first : second;

        validateAccount(source);
        validateAccount(destination);

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for transfer");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        return saveTransaction(
                source,
                amount,
                TransactionType.TRANSFER,
                transactionId);
    }

    // VALIDATE AMOUNT
    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransactionException(
                    "Amount must be greater than zero");
        }
    }

    // VALIDATE TRANSACTION ID
    private void validateTransactionId(String transactionId) {

        if (transactionId == null || transactionId.isBlank()) {

            throw new InvalidTransactionException(
                    "Transaction ID is required");
        }
    }

    // CHECK DUPLICATE TRANSACTION
    private void checkDuplicate(String transactionId) {

        if (transactionRepository
                .findByTransactionId(transactionId).isPresent()) {

            throw new DuplicateTransactionException(
                    "Duplicate transaction ID");
        }
    }

    // VALIDATE ACCOUNT STATUS
    private void validateAccount(Account account) {

        if (account.getStatus() != AccountStatus.ACTIVE) {

            throw new AccountInactiveException(
                    "Account is not active");
        }
    }

    // SAVE TRANSACTION AND AUDIT
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

        BankTransaction savedTransaction =
                transactionRepository.save(transaction);

        saveAudit(
                transactionId,
                type.name(),
                "SUCCESS",
                "Transaction completed successfully"
        );

        return savedTransaction;
    }

    // SAVE AUDIT RECORD
    private void saveAudit(
            String transactionId,
            String operation,
            String result,
            String details) {

        TransactionAudit audit = new TransactionAudit();

        audit.setTransactionId(transactionId);
        audit.setOperation(operation);
        audit.setResult(result);
        audit.setDetails(details);

        auditRepository.save(audit);
    }
}
