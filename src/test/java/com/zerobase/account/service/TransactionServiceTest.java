package com.zerobase.account.service;

import com.zerobase.account.domain.Account;
import com.zerobase.account.domain.AccountUser;
import com.zerobase.account.domain.Transaction;
import com.zerobase.account.dto.TransactionDto;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.repository.AccountRepository;
import com.zerobase.account.repository.AccountUserRepository;
import com.zerobase.account.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.zerobase.account.type.AccountStatus.IN_USE;
import static com.zerobase.account.type.AccountStatus.UNREGISTERED;
import static com.zerobase.account.type.ErrorCode.*;
import static com.zerobase.account.type.TransactionResultType.FAIL;
import static com.zerobase.account.type.TransactionResultType.SUCCESS;
import static com.zerobase.account.type.TransactionType.CANCEL;
import static com.zerobase.account.type.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.useBalance(1L,
                "1000000000", USE_AMOUNT);

        verify(transactionRepository, times(1)).save(captor.capture());

        // then
        assertThat(USE_AMOUNT).isEqualTo(captor.getValue().getAmount());
        assertThat(9800L).isEqualTo(captor.getValue().getBalanceSnapshot());

        assertThat(SUCCESS).isEqualTo(transactionDto.getTransactionResultType());
        assertThat(USE).isEqualTo(transactionDto.getTransactionType());
        assertThat(9000L).isEqualTo(transactionDto.getBalanceSnapshot());
        assertThat(1000L).isEqualTo(transactionDto.getAmount());

    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UerNotFound() {
        // given

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000000", 1000L));

        // then
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void deleteAccount_AccountNotFound() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 10000L));

        assertThat(ACCOUNT_NOT_FOUND).isEqualTo(accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void useBalance_userUnMatch() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        AccountUser harry = AccountUser.builder()
                .name("Harry").build();
        harry.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertThat(USER_ACCOUNT_UN_MATCH).isEqualTo(accountException.getErrorCode());
    }
    @Test
    @DisplayName("해지 계좌는 사용할 수 없습니다.")
    void useBalance_alreadyUnRegistered() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(0L)
                        .accountStatus(UNREGISTERED)
                        .accountNumber("1000000012").build()));
        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertThat(ACCOUNT_ALREADY_UNREGISTERED).isEqualTo(accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_useBalance() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        // then
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertThat(AMOUNT_EXCEED_BALANCE).isEqualTo(accountException.getErrorCode());

        verify(transactionRepository, times(0)).save(any());

    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        verify(transactionRepository, times(1)).save(captor.capture());

        // then
        assertThat(USE_AMOUNT).isEqualTo(captor.getValue().getAmount());
        assertThat(10000L).isEqualTo(captor.getValue().getBalanceSnapshot());
        assertThat(FAIL).isEqualTo(captor.getValue().getTransactionResultType());
    }


    @Test
    void successCancelBalance() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT);

        verify(transactionRepository, times(1)).save(captor.capture());

        // then
        assertThat(CANCEL_AMOUNT).isEqualTo(captor.getValue().getAmount());
        assertThat(10000L + CANCEL_AMOUNT).isEqualTo(captor.getValue().getBalanceSnapshot());

        assertThat(SUCCESS).isEqualTo(transactionDto.getTransactionResultType());
        assertThat(CANCEL).isEqualTo(transactionDto.getTransactionType());
        assertThat(10000L).isEqualTo(transactionDto.getBalanceSnapshot());
        assertThat(CANCEL_AMOUNT).isEqualTo(transactionDto.getAmount());

    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        // given

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 10000L));

        assertThat(ACCOUNT_NOT_FOUND).isEqualTo(accountException.getErrorCode());
    }


    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        // given

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 10000L));

        assertThat(TRANSACTION_NOT_FOUND).isEqualTo(accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌가 매칭 실패 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionAccountUnMatch() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013")
                .build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", CANCEL_AMOUNT));

        assertThat(TRANSACTION_ACCOUNT_UN_MATCH).isEqualTo(accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_CancelMustFully() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", CANCEL_AMOUNT));

        assertThat(CANCEL_MUST_FULLY).isEqualTo(accountException.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrderToCancel() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", CANCEL_AMOUNT));

        assertThat(TOO_OLD_ORDER_TO_CANCEL).isEqualTo(accountException.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi").build();
        accountUser.setId(12L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        // then
        assertThat(USE).isEqualTo(transactionDto.getTransactionType());
        assertThat(SUCCESS).isEqualTo(transactionDto.getTransactionResultType());
        assertThat(CANCEL_AMOUNT).isEqualTo(transactionDto.getAmount());
        assertThat("transactionId").isEqualTo(transactionDto.getTransactionId());

    }

    @Test
    @DisplayName("원거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        assertThat(TRANSACTION_NOT_FOUND).isEqualTo(accountException.getErrorCode());
    }
}

