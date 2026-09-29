package com.payflow.transfers;

import org.springframework.data.domain.Page;

import java.util.List;

public record TransferPageResponse(
        List<TransferResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    static TransferPageResponse from(Page<Transfer> transfers) {
        return new TransferPageResponse(
                transfers.getContent().stream().map(TransferResponse::from).toList(),
                transfers.getNumber(),
                transfers.getSize(),
                transfers.getTotalElements(),
                transfers.getTotalPages(),
                transfers.isFirst(),
                transfers.isLast()
        );
    }
}
