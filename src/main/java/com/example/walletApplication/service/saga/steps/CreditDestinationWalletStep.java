package com.example.walletApplication.service.saga.steps;

import com.example.walletApplication.entity.Wallet;
import com.example.walletApplication.repository.WalletRepository;
import com.example.walletApplication.service.saga.SagaContext;
import com.example.walletApplication.service.saga.SagaStep;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditDestinationWalletStep implements SagaStep {
    private final WalletRepository walletRepository;


    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Crediting destination wallet {} with amount {} ", toWalletId, amount);

        Wallet wallet = walletRepository.findByIdWithLock(toWalletId).orElseThrow( () -> new RuntimeException("wallet not found"));
        log.info("Wallet fetched with balance: {}", wallet.getBalance());
        context.put("originalToWalletBalance", wallet.getBalance());

        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Wallet save with balance: {}",wallet.getBalance());
        context.put("toWalletBalanceAfterCredit", wallet.getBalance());

        log.info("Credit destination wallet step executed successfully !!");
        return true;
    }

    @Override
    public boolean compensate(SagaContext context) {
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Compensating credit of destination wallet {} with amount {} ", toWalletId, amount);

        Wallet wallet = walletRepository.findByIdWithLock(toWalletId).orElseThrow( () -> new RuntimeException("wallet not found"));
        log.info("Wallet fetched with balance: {}", wallet.getBalance());

        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("Wallet save with balance: {}",wallet.getBalance());
        context.put("toWalletBalanceAfterCreditCompensated", wallet.getBalance());

        log.info("Credit compensation of destination wallet step executed successfully !!");
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepFactory.SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString();
    }
}
