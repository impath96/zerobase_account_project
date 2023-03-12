package com.zerobase.account.exception;

// checked exception : transaction rollback 대상이 아니다 ??

// AccountNotFoundException, AccountUserException 등 각각의 예외 상황에 따른
// 클래스를 설계할 수도 있지만 이러면 너무 번거롭기 때문에 ErrorCode 활용

import com.zerobase.account.type.ErrorCode;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountException extends RuntimeException {

    private ErrorCode errorCode;
    private String errorMessage;

    public AccountException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }

}
