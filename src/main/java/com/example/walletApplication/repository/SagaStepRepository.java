package com.example.walletApplication.repository;

import com.example.walletApplication.entity.SagaStep;
import com.example.walletApplication.entity.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    List<SagaStep> findBySagaInstanceIdAndStepStatus(Long sagaInstanceId, StepStatus stepStatus);

    List<SagaStep> findByInstanceIdAndStepNameAndStepStatus(Long sagaInstanceId, String stepName, StepStatus stepStatus);

    @Query("select s from SagaStep s where s.stepStatus = 'COMPLETED' and s.sagaInstanceId = :instanceId")
    List<SagaStep> findCompletedStepsByInstanceId(@Param("instanceId") Long instanceId);

}
