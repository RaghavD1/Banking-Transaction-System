
package com.newgen.banking.repository;

import com.newgen.banking.entity.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionAuditRepository
        extends JpaRepository<TransactionAudit, Long> {

    List<TransactionAudit> findByTransactionId(String transactionId);
}
