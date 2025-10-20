package com.example.walletApplication.repository;

import com.example.walletApplication.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

}
