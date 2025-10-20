package com.example.walletApplication.service.saga;

import com.example.walletApplication.entity.SagaInstance;
import com.example.walletApplication.entity.SagaStatus;
import com.example.walletApplication.entity.SagaStep;
import com.example.walletApplication.entity.StepStatus;
import com.example.walletApplication.repository.SagaInstanceRepository;
import com.example.walletApplication.repository.SagaStepRepository;
import com.example.walletApplication.service.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepFactory sagaStepFactory;
    private final SagaStepRepository sagaStepRepository;

    @Override
    public Long startSaga(SagaContext context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            SagaInstance sagaInstance = SagaInstance.builder().context(contextJson).sagaStatus(SagaStatus.STARTED).build();

            sagaInstanceRepository.save(sagaInstance);
            return sagaInstance.getId();
        } catch (Exception e) {
            throw new RuntimeException("Error starting saga", e);
        }
    }

    @Override
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if(null == step) {
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStep = sagaStepRepository.findBySagaInstanceIdAndStepStatus(sagaInstanceId, StepStatus.PENDING)
                .stream()
                .filter(s -> s.getStepName().equals(stepName))
                .findFirst()
                .orElse(SagaStep.builder().sagaInstanceId(sagaInstanceId).stepStatus(StepStatus.PENDING).stepName(stepName).build());

        if(sagaStep.getId() == null) {
            sagaStepRepository.save(sagaStep);
        }

        try {
            SagaContext context = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStep.setStepStatus(StepStatus.RUNNING);
            sagaStepRepository.save(sagaStep);

            boolean success = step.execute(context);

            if(success) {
                log.info("Step {} executed successfully ", stepName);
                sagaStep.setStepStatus(StepStatus.COMPLETED);
                sagaStepRepository.save(sagaStep);

                sagaInstance.setCurrentStep(stepName);
                sagaInstance.setSagaStatus(SagaStatus.RUNNING);
                sagaInstanceRepository.save(sagaInstance);

                return true;
            } else {
                log.error("Step {} failed", stepName);
                sagaStep.setStepStatus(StepStatus.FAILED);
                sagaStepRepository.save(sagaStep);

                return false;
            }
        } catch(Exception e) {
            log.error("Error reading saga context ", e);
            sagaStep.setStepStatus(StepStatus.FAILED);
            sagaStepRepository.save(sagaStep);
            throw new RuntimeException("Error reading saga context ");
        }

    }

    @Override
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        return false;
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return null;
    }

    @Override
    public void compensateSaga(Long sagaInstanceId) {

    }

    @Override
    public void failSaga(Long sagaInstanceId) {

    }

    @Override
    public void completeSaga(Long sagaInstanceId) {

    }
}
