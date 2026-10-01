package com.payflow.transfers;

public interface TransferEventRecorder {

    void recordCompleted(Transfer transfer, String correlationId);

    void recordReversed(Transfer reversal, String correlationId);
}
