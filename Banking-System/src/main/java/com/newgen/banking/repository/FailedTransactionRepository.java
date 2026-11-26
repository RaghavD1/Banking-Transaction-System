
package com.newgen.banking.repository;

import com.newgen.banking.entity.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedTransactionRepository
        extends JpaRepository<FailedTransaction, Long> {

    List<FailedTransaction> findByTransactionId(String transactionId);
}
