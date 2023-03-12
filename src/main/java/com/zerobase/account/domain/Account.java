package com.zerobase.account.domain;

import com.zerobase.account.exception.AccountException;
import com.zerobase.account.type.AccountStatus;
import com.zerobase.account.type.ErrorCode;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

// @CreatedDate, @LastModifiedDate를 사용하기 위해서
// @EntityListeners(AuditingEntityListener.class) 추가!
// 왜 이렇게 사용하는가?
// AuditingEntityListener는 어플리케이션 설정이 필요하다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account extends BaseEntity{

    // 그냥 user일 경우 DB에 user라는 테이블과 충돌이 일어날 수 있기 때문이다.
    @ManyToOne
    private AccountUser accountUser;
    private String accountNumber;

    // Enum 값의 실제 문자열 그대로 저장 (이렇게 안하면 숫자 들어감)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public void useBalance(Long amount) {
        if(amount > this.balance) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }

        this.balance -= amount;
    }

    public void cancelBalance(Long amount) {
        if(amount < 0) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }
        this.balance += amount;
    }
}
