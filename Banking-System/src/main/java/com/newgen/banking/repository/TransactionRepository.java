
package com.newgen.banking.repository;

import com.newgen.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionId(String transactionId);

    List<BankTransaction> findByAccountAccountNumberOrderByCreatedAtDesc(
            String accountNumber);
}
