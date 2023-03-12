package com.zerobase.account.dto;

import com.zerobase.account.aop.AccountLockIdInterface;
import com.zerobase.account.type.TransactionResultType;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class UseBalance {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Request implements AccountLockIdInterface {
        @NotNull
        @Min(1)
        private Long userId;

        @NotBlank
        @Size(min = 10, max = 10)
        private String accountNumber;

        @NotNull
        @Min(10)
        @Max(1000_000_000)
        private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String accountNumber;
        private TransactionResultType transactionResult;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        // DTO -> Response
        public static Response from(TransactionDto accountDto) {
            return Response.builder()
                    .accountNumber(accountDto.getAccountNumber())
                    .transactionResult(accountDto.getTransactionResultType())
                    .transactionId(accountDto.getTransactionId())
                    .amount(accountDto.getAmount())
                    .transactedAt(accountDto.getTransactedAt())
                    .build();
        }
    }

}
