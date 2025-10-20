package com.example.walletApplication.service.saga.steps;

import com.example.walletApplication.entity.Transaction;
import com.example.walletApplication.entity.TransactionStatus;
import com.example.walletApplication.repository.TransactionRepository;
import com.example.walletApplication.service.saga.SagaContext;
import com.example.walletApplication.service.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatus implements SagaStep {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean execute(SagaContext context) {
        Long transactionId = context.getLong("transactionId");
        log.info("Updating transaction status of transaction: {}",transactionId);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("transaction not found"));
        context.put("originalTransactionStatus", transaction.getTransactionStatus());

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
        context.put("transactionStatusAfterUpdate", transaction.getTransactionStatus());
        return true;
    }

    @Override
    public boolean compensate(SagaContext context) {
        Long transactionId = context.getLong("transactionId");
        log.info("Updating transaction status of transaction: {}",transactionId);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("transaction not found"));

        String originalTransactionStatus = context.getString("originalTransactionStatus");
         transaction.setTransactionStatus(TransactionStatus.valueOf(originalTransactionStatus));
        transactionRepository.save(transaction);
        context.put("transactionStatusAfterUpdateCompensation", transaction.getTransactionStatus());
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString();
    }
}
