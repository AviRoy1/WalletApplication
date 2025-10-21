package com.example.walletApplication.service.saga;

import com.example.walletApplication.entity.SagaInstance;
import com.example.walletApplication.entity.SagaStatus;
import com.example.walletApplication.entity.SagaStep;
import com.example.walletApplication.entity.StepStatus;
import com.example.walletApplication.repository.SagaInstanceRepository;
import com.example.walletApplication.repository.SagaStepRepository;
import com.example.walletApplication.service.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
    @Transactional
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
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if(null == step) {
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStep = sagaStepRepository.findByInstanceIdAndStepNameAndStepStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                .stream().findFirst()
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
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = this.getSagaInstance(sagaInstanceId);
        SagaStepInterface sagaStepInterface = sagaStepFactory.getSagaStep(stepName);

        if(null == sagaStepInterface) {
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStep = sagaStepRepository.findByInstanceIdAndStepNameAndStepStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                .stream().findFirst()
                .orElse(null);

        if(sagaStep == null) {
            log.info("No steps founds in db, so it is already compensated.");
            return true;
        }

        try{
            SagaContext context = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStep.setStepStatus(StepStatus.COMPENSATING);
            sagaStepRepository.save(sagaStep);

            boolean isSuccess = sagaStepInterface.compensate(context);
            if(isSuccess) {
                sagaStep.setStepStatus(StepStatus.COMPENSATED);
                sagaStepRepository.save(sagaStep);
                return true;
            } else {
                log.error("Step {} failed", stepName);
                sagaStep.setStepStatus(StepStatus.FAILED);
                sagaStepRepository.save(sagaStep);

                return false;
            }

        }catch(Exception e) {
            log.error("Error reading saga context ", e);
            sagaStep.setStepStatus(StepStatus.FAILED);
            sagaStepRepository.save(sagaStep);
            throw new RuntimeException("Error reading saga context ");
        }
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException("Saga instance not found."));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

        sagaInstance.setSagaStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(sagaInstance);

        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsByInstanceId(sagaInstanceId);

        boolean allCompensated = true;
        for(SagaStep completedStep : completedSteps) {
            boolean compensated = this.compensateStep(completedStep.getSagaInstanceId(), completedStep.getStepName());
            if(!compensated) {
                allCompensated = false;
            }
        }

        if(allCompensated) {
            sagaInstance.setSagaStatus(SagaStatus.COMPENSATED);
            sagaInstanceRepository.save(sagaInstance);
            log.info("Saga {} compensated successfully", sagaInstanceId);
        } else {
            log.error("Saga {} compensation failed", sagaInstanceId);
        }

    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);
        sagaInstance.setSagaStatus(SagaStatus.FAILED);
        sagaInstanceRepository.save(sagaInstance);

        compensateSaga(sagaInstanceId);
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);
        sagaInstance.setSagaStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(sagaInstance);
    }
}
