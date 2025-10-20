package com.example.walletApplication.config;

import com.example.walletApplication.service.saga.SagaStep;
import com.example.walletApplication.service.saga.steps.CreditDestinationWalletStep;
import com.example.walletApplication.service.saga.steps.DebitSourceWalletStep;
import com.example.walletApplication.service.saga.steps.SagaStepFactory;
import com.example.walletApplication.service.saga.steps.UpdateTransactionStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagaConfig {

    @Bean
    public Map<String , SagaStep> sagaStepMap(
            DebitSourceWalletStep debitSourceWalletStep,
            CreditDestinationWalletStep creditDestinationWalletStep,
            UpdateTransactionStatus updateTransactionStatus
    ) {

        Map<String, SagaStep> map = new HashMap<>();
        map.put(SagaStepFactory.SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString(), creditDestinationWalletStep);
        map.put(SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString(), debitSourceWalletStep);
        map.put(SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString(), updateTransactionStatus);
        return map;
    }

}
