package com.example.walletApplication.service.saga;

public interface SagaStepInterface {

    boolean execute(SagaContext context);

    boolean compensate(SagaContext context);

    String getStepName();
}
