package com.example.walletApplication.service.saga;

import com.example.walletApplication.entity.SagaInstance;

public interface SagaOrchestrator {

    Long startSaga(SagaContext context);

    boolean executeStep(Long sagaInstanceId, String stepName);

    boolean compensateStep(Long sagaInstanceId, String stepName);

    SagaInstance getSagaInstance(Long sagaInstanceId);

    void compensateSaga(Long sagaInstanceId);

    void failSaga(Long sagaInstanceId);

    void completeSaga(Long sagaInstanceId);


}
