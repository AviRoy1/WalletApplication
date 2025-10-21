package com.example.walletApplication.service;

import com.example.walletApplication.entity.Wallet;
import com.example.walletApplication.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final WalletRepository walletRepository;

    public Wallet createWallet(Long userId) {
        Wallet wallet = Wallet.builder().userId(userId).isActive(true).balance(BigDecimal.ZERO).build();
        return walletRepository.save(wallet);
    }

    public Wallet getWalletById(Long walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public void debit(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        wallet.debit(amount);
        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        wallet.credit(amount);
        walletRepository.save(wallet);
    }

    public BigDecimal getWalletBalance(Long walletId) {
        return getWalletById(walletId).getBalance();
    }

}
