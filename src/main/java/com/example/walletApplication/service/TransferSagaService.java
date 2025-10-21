package com.example.walletApplication.service;

import com.example.walletApplication.entity.SagaInstance;
import com.example.walletApplication.entity.Transaction;
import com.example.walletApplication.service.saga.SagaContext;
import com.example.walletApplication.service.saga.SagaOrchestrator;
import com.example.walletApplication.service.saga.steps.SagaStepFactory;
import com.example.walletApplication.service.saga.steps.SagaStepFactory.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaService {

    private final TransactionService transactionService;
    private final SagaOrchestrator sagaOrchestrator;

    @Transactional
    public Long initiateTransfer(Long fromWalletId, Long toWalletId, BigDecimal amount, String description) {
        Transaction transaction = transactionService.createTransaction(fromWalletId, toWalletId, amount, description);
        SagaContext sagaContext = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("toWalletId", toWalletId),
                        Map.entry("fromWalletId", fromWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)
                ))
                .build();
        Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);
        log.info("Saga instance create with id : {}", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        executeTransferSaga(sagaInstanceId);
        return sagaInstanceId;
    }

    public void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing saga with id : {}",sagaInstanceId);

        try {
            for(SagaStepType step : SagaStepFactory.TransferMoneySagaSteps) {
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.toString());
                if(!success) {
                    log.error("Failed to execute the step : {}", step);
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }
            }
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed with id : {}", sagaInstanceId);
        } catch(Exception e) {
            log.error("Failed to execute transfer saga with id : {}", sagaInstanceId, e);
            sagaOrchestrator.failSaga(sagaInstanceId);
        }
    }

}
