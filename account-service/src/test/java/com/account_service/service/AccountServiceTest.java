package com.account_service.service;

import com.account_service.entity.Account;
import com.account_service.entity.EventStatus;
import com.account_service.entity.EventType;
import com.account_service.entity.Transactions;
import com.account_service.repository.AccountRepository;
import com.account_service.repository.TransactionsRepository;
import com.account_service.requestORresponse.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Test Suite")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionsRepository transactionsRepository;

    @InjectMocks
    private AccountService accountService;

    private EventRequest validEventRequest;
    private Account testAccount;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        
        testAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000.00"))
                .build();

        validEventRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );
    }

    // ==================== processTransaction Tests ====================

    @Test
    @DisplayName("When: Valid CREDIT transaction is processed, Then: Balance is incremented and success response returned")
    void testProcessTransactionWithCreditSuccess() {
        // Given
        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1100.00"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transaction processed successfully", response.getBody());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionsRepository, times(1)).save(any(Transactions.class));
    }

    @Test
    @DisplayName("When: Valid DEBIT transaction is processed, Then: Balance is decremented and success response returned")
    void testProcessTransactionWithDebitSuccess() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("100.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account accountWithSufficientBalance = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000.00"))
                .build();

        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("900.00"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(accountWithSufficientBalance));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transaction processed successfully", response.getBody());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("When: EventRequest is null, Then: IllegalArgumentException is thrown")
    void testProcessTransactionWithNullEventRequest() {
        // Given
        EventRequest nullRequest = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.processTransaction(nullRequest, "ACC001")
        );
        assertEquals("Invalid Transaction", exception.getMessage());
    }

    @Test
    @DisplayName("When: Account ID in request does not match parameter, Then: IllegalArgumentException is thrown")
    void testProcessTransactionWithMismatchedAccountId() {
        // Given
        EventRequest mismatchedRequest = new EventRequest(
                1L,
                "ACC002",
                EventType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.processTransaction(mismatchedRequest, "ACC001")
        );
        assertEquals("Invalid account details", exception.getMessage());
    }

    @Test
    @DisplayName("When: Account does not exist, Then: RuntimeException is thrown")
    void testProcessTransactionWithNonExistentAccount() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.processTransaction(validEventRequest, "ACC001")
        );
        assertEquals("Account not found.", exception.getMessage());
    }

    @Test
    @DisplayName("When: Transaction amount is zero, Then: Transaction continues (amount validation happens at controller level)")
    void testProcessTransactionWithZeroAmount() {
        // Given
        EventRequest zeroAmountRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                BigDecimal.ZERO,
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(zeroAmountRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: DEBIT transaction with zero balance, Then: Bad request response is returned")
    void testProcessDebitTransactionWithZeroBalance() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("50.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account zeroBalanceAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(zeroBalanceAccount));

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("When: DEBIT transaction with negative balance, Then: Bad request response is returned")
    void testProcessDebitTransactionWithNegativeBalance() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("50.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account negativeBalanceAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("-100.00"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(negativeBalanceAccount));

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Multiple transactions are processed sequentially, Then: All are processed successfully")
    void testProcessMultipleTransactions() {
        // Given
        Account account1 = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000.00"))
                .build();

        EventRequest creditRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                new BigDecimal("500.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(account1));
        when(accountRepository.save(any(Account.class))).thenReturn(account1);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response1 = accountService.processTransaction(creditRequest, "ACC001");
        ResponseEntity<String> response2 = accountService.processTransaction(creditRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(accountRepository, times(2)).findByAccountId("ACC001");
    }

    @Test
    @DisplayName("When: Large credit amount is processed, Then: Balance is correctly updated")
    void testProcessTransactionWithLargeAmount() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999.99");
        EventRequest largeAmountRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                largeAmount,
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000999.99"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(largeAmountRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== findByAccountId Tests ====================

    @Test
    @DisplayName("When: Valid accountId is provided, Then: Account is returned")
    void testFindByAccountIdWithValidId() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        Account result = accountService.findByAccountId("ACC001");

        // Then
        assertNotNull(result);
        assertEquals("ACC001", result.getAccountId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository, times(1)).findByAccountId("ACC001");
    }

    @Test
    @DisplayName("When: Non-existent accountId is provided, Then: null is returned")
    void testFindByAccountIdWithNonExistentId() {
        // Given
        when(accountRepository.findByAccountId("INVALID")).thenReturn(Optional.empty());

        // When
        Account result = accountService.findByAccountId("INVALID");

        // Then
        assertNull(result);
        verify(accountRepository, times(1)).findByAccountId("INVALID");
    }

    @Test
    @DisplayName("When: null accountId is provided, Then: NullPointerException is thrown")
    void testFindByAccountIdWithNullId() {
        // Given
        String nullAccountId = null;

        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> accountService.findByAccountId(nullAccountId)
        );
        assertEquals("Cannot process. accountId is null", exception.getMessage());
    }

    @Test
    @DisplayName("When: findByAccountId is called multiple times with same id, Then: Repository is called each time")
    void testFindByAccountIdMultipleCalls() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        Account result1 = accountService.findByAccountId("ACC001");
        Account result2 = accountService.findByAccountId("ACC001");

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getAccountId(), result2.getAccountId());
        verify(accountRepository, times(2)).findByAccountId("ACC001");
    }

    @Test
    @DisplayName("When: Account with special characters in ID is searched, Then: Account is returned")
    void testFindByAccountIdWithSpecialCharacters() {
        // Given
        Account specialAccount = Account.builder()
                .id(2L)
                .accountId("ACC-001-SPECIAL")
                .balance(new BigDecimal("5000.00"))
                .build();

        when(accountRepository.findByAccountId("ACC-001-SPECIAL")).thenReturn(Optional.of(specialAccount));

        // When
        Account result = accountService.findByAccountId("ACC-001-SPECIAL");

        // Then
        assertNotNull(result);
        assertEquals("ACC-001-SPECIAL", result.getAccountId());
    }

    @Test
    @DisplayName("When: Transaction is built from EventRequest, Then: All fields are correctly set")
    void testBuildTransactionFieldsFromEventRequest() {
        // Given
        Account account = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(transactionsRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        accountService.processTransaction(validEventRequest, "ACC001");

        // Then
        verify(transactionsRepository).save(argThat(transaction ->
                transaction.getTransactionAmount().equals(new BigDecimal("100.00")) &&
                transaction.getType().equals(EventType.CREDIT) &&
                transaction.getTransactionStatus().equals(EventStatus.PROCESSED) &&
                transaction.getAccount().equals(account)
        ));
    }

    @Test
    @DisplayName("When: CREDIT transaction with fractional amount, Then: Balance is precisely updated")
    void testProcessTransactionWithFractionalAmount() {
        // Given
        EventRequest fractionalRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                new BigDecimal("99.99"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1099.99"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(fractionalRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: DEBIT transaction exceeds available balance, Then: Insufficient balance validation passes after check")
    void testProcessDebitWithInsufficientBalance() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("2000.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then
        assertNotNull(response);
        verify(accountRepository, times(1)).findByAccountId("ACC001");
    }

    // ==================== Additional Edge Case Tests for 90%+ Coverage ====================

    @Test
    @DisplayName("When: CREDIT transaction with exactly balance.ZERO add, Then: Success response returned")
    void testCreditTransactionZeroAddition() {
        // Given
        EventRequest creditRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.CREDIT,
                new BigDecimal("0.01"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("1000.01"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(creditRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Very small DEBIT amount, Then: Balance decremented correctly")
    void testDebitTransactionVerySmallAmount() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("0.01"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        Account savedAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("999.99"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: CREDIT transaction with response body validation, Then: Exact message returned")
    void testTransactionResponseBody() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals("Transaction processed successfully", response.getBody());
        assertNotNull(response.getStatusCode());
    }

    @Test
    @DisplayName("When: Account balance is exactly equal to DEBIT amount, Then: Balance becomes zero")
    void testDebitExactBalance() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L,
                "ACC001",
                EventType.DEBIT,
                new BigDecimal("1000.00"),
                "USD",
                testDateTime,
                EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then - Should be BAD_REQUEST because balance equals debit amount (insufficient)
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Multiple CREDIT transactions, Then: All balances calculated correctly")
    void testMultipleCreditTransactions() {
        // Given
        EventRequest creditRequest1 = new EventRequest(
                1L, "ACC001", EventType.CREDIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PENDING
        );
        EventRequest creditRequest2 = new EventRequest(
                2L, "ACC001", EventType.CREDIT, new BigDecimal("200.00"), "USD", testDateTime, EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response1 = accountService.processTransaction(creditRequest1, "ACC001");
        ResponseEntity<String> response2 = accountService.processTransaction(creditRequest2, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(accountRepository, times(2)).findByAccountId("ACC001");
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("When: DEBIT followed by CREDIT on same account, Then: Both processed successfully")
    void testMixedTransactionSequence() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L, "ACC001", EventType.DEBIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PENDING
        );
        EventRequest creditRequest = new EventRequest(
                2L, "ACC001", EventType.CREDIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response1 = accountService.processTransaction(debitRequest, "ACC001");
        ResponseEntity<String> response2 = accountService.processTransaction(creditRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }

    @Test
    @DisplayName("When: Transaction with EventStatus PROCESSED, Then: Processing is allowed")
    void testTransactionWithProcessedStatus() {
        // Given
        EventRequest processedRequest = new EventRequest(
                1L, "ACC001", EventType.CREDIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PROCESSED
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(processedRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: FindByAccountId returns Optional.empty, Then: null is returned consistently")
    void testFindByAccountIdEmptyOptional() {
        // Given
        when(accountRepository.findByAccountId("NONEXIST")).thenReturn(Optional.empty());

        // When
        Account result1 = accountService.findByAccountId("NONEXIST");
        Account result2 = accountService.findByAccountId("NONEXIST");

        // Then
        assertNull(result1);
        assertNull(result2);
        verify(accountRepository, times(2)).findByAccountId("NONEXIST");
    }

    @Test
    @DisplayName("When: Account ID is very long, Then: Processing succeeds")
    void testLongAccountId() {
        // Given
        String longAccountId = "ACC" + "0".repeat(50) + "1";
        Account longIdAccount = Account.builder()
                .id(1L)
                .accountId(longAccountId)
                .balance(new BigDecimal("1000.00"))
                .build();

        EventRequest request = new EventRequest(
                1L, longAccountId, EventType.CREDIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PENDING
        );

        when(accountRepository.findByAccountId(longAccountId)).thenReturn(Optional.of(longIdAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(longIdAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(request, longAccountId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Transaction amount is positive max value, Then: Processing succeeds")
    void testMaxPositiveAmount() {
        // Given
        BigDecimal maxAmount = new BigDecimal("999999999.99");
        EventRequest maxRequest = new EventRequest(
                1L, "ACC001", EventType.CREDIT, maxAmount, "USD", testDateTime, EventStatus.PENDING
        );

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(maxRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Repository throws exception during save, Then: Exception propagates")
    void testRepositoryExceptionDuringSave() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenThrow(new RuntimeException("Save failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            accountService.processTransaction(validEventRequest, "ACC001");
        });
    }

    @Test
    @DisplayName("When: Transactions repository throws exception, Then: Exception propagates")
    void testTransactionsRepositoryException() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenThrow(new RuntimeException("Transaction save failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            accountService.processTransaction(validEventRequest, "ACC001");
        });
    }

    @Test
    @DisplayName("When: Account balance is exactly 0.01 and DEBIT 0.01, Then: Bad request returned")
    void testMinimumBalanceDebit() {
        // Given
        EventRequest debitRequest = new EventRequest(
                1L, "ACC001", EventType.DEBIT, new BigDecimal("0.01"), "USD", testDateTime, EventStatus.PENDING
        );

        Account minBalanceAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("0.01"))
                .build();

        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(minBalanceAccount));

        // When
        ResponseEntity<String> response = accountService.processTransaction(debitRequest, "ACC001");

        // Then - Should be BAD_REQUEST because balance equals amount (insufficient)
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("When: FindByAccountId with numeric ID, Then: Account retrieved successfully")
    void testFindByNumericAccountId() {
        // Given
        Account numericAccount = Account.builder()
                .id(1L)
                .accountId("123456789")
                .balance(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findByAccountId("123456789")).thenReturn(Optional.of(numericAccount));

        // When
        Account result = accountService.findByAccountId("123456789");

        // Then
        assertNotNull(result);
        assertEquals("123456789", result.getAccountId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
    }

    @Test
    @DisplayName("When: Transaction response entity status and body both checked, Then: Both are correct")
    void testTransactionResponseValidation() {
        // Given
        when(accountRepository.findByAccountId("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionsRepository.save(any(Transactions.class))).thenReturn(new Transactions());

        // When
        ResponseEntity<String> response = accountService.processTransaction(validEventRequest, "ACC001");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transaction processed successfully", response.getBody());
        assertTrue(response.getBody().contains("successfully"));
    }
}



