package com.account_service.controller;

import com.account_service.entity.Account;
import com.account_service.entity.EventStatus;
import com.account_service.entity.EventType;
import com.account_service.requestORresponse.EventRequest;
import com.account_service.service.AccountService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController Test Suite")
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

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

    @Test
    @DisplayName("When: Valid transaction request is submitted, Then: Transaction is processed and response is OK")
    void testCreateTransactionSuccess() {
        // Given
        when(accountService.processTransaction(validEventRequest, "ACC001"))
                .thenReturn(ResponseEntity.ok("Transaction processed successfully"));

        // When
        ResponseEntity<String> response = accountController.createTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transaction processed successfully", response.getBody());
        verify(accountService, times(1)).processTransaction(validEventRequest, "ACC001");
    }

    @Test
    @DisplayName("When: Invalid transaction throws exception, Then: Bad request response with error message is returned")
    void testCreateTransactionWithException() {
        // Given
        when(accountService.processTransaction(validEventRequest, "ACC001"))
                .thenThrow(new IllegalArgumentException("Invalid account details"));

        // When
        ResponseEntity<String> response = accountController.createTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Invalid account details"));
        verify(accountService, times(1)).processTransaction(validEventRequest, "ACC001");
    }

    @Test
    @DisplayName("When: Account not found exception is thrown, Then: Bad request with error message is returned")
    void testCreateTransactionAccountNotFound() {
        // Given
        when(accountService.processTransaction(validEventRequest, "ACC001"))
                .thenThrow(new RuntimeException("Account not found."));

        // When
        ResponseEntity<String> response = accountController.createTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Account not found"));
    }

    @Test
    @DisplayName("When: Valid accountId is provided, Then: Account details are returned with OK status")
    void testGetAccountByAccountIdSuccess() {
        // Given
        when(accountService.findByAccountId("ACC001")).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.getAccountByAccountId("ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACC001", response.getBody().getAccountId());
        assertEquals(new BigDecimal("1000.00"), response.getBody().getBalance());
        verify(accountService, times(1)).findByAccountId("ACC001");
    }

    @Test
    @DisplayName("When: Non-existent accountId is provided, Then: NOT_FOUND status is returned")
    void testGetAccountByAccountIdNotFound() {
        // Given
        when(accountService.findByAccountId("INVALID")).thenReturn(null);

        // When
        ResponseEntity<Account> response = accountController.getAccountByAccountId("INVALID");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(accountService, times(1)).findByAccountId("INVALID");
    }

    @Test
    @DisplayName("When: Valid accountId is provided for balance, Then: Balance is returned with OK status")
    void testGetBalanceSuccess() {
        // Given
        when(accountService.findByAccountId("ACC001")).thenReturn(testAccount);

        // When
        ResponseEntity<BigDecimal> response = accountController.getBalance("ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("1000.00"), response.getBody());
        verify(accountService, times(1)).findByAccountId("ACC001");
    }

    @Test
    @DisplayName("When: Non-existent accountId is provided for balance, Then: NOT_FOUND status is returned")
    void testGetBalanceNotFound() {
        // Given
        when(accountService.findByAccountId("INVALID")).thenReturn(null);

        // When
        ResponseEntity<BigDecimal> response = accountController.getBalance("INVALID");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(accountService, times(1)).findByAccountId("INVALID");
    }

    @Test
    @DisplayName("When: Multiple valid account IDs are requested, Then: All are retrieved successfully")
    void testGetMultipleAccounts() {
        // Given
        Account account1 = Account.builder().id(1L).accountId("ACC001").balance(new BigDecimal("1000.00")).build();
        Account account2 = Account.builder().id(2L).accountId("ACC002").balance(new BigDecimal("2000.00")).build();

        when(accountService.findByAccountId("ACC001")).thenReturn(account1);
        when(accountService.findByAccountId("ACC002")).thenReturn(account2);

        // When
        ResponseEntity<Account> response1 = accountController.getAccountByAccountId("ACC001");
        ResponseEntity<Account> response2 = accountController.getAccountByAccountId("ACC002");

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals("ACC001", response1.getBody().getAccountId());
        assertEquals("ACC002", response2.getBody().getAccountId());
        verify(accountService, times(1)).findByAccountId("ACC001");
        verify(accountService, times(1)).findByAccountId("ACC002");
    }

    @Test
    @DisplayName("When: DEBIT transaction with insufficient balance, Then: Bad request is returned")
    void testCreateTransactionDebitInsufficientBalance() {
        // Given
        when(accountService.processTransaction(any(EventRequest.class), eq("ACC001")))
                .thenReturn(ResponseEntity.badRequest().body("Insufficient balance"));

        EventRequest debitRequest = new EventRequest(
                1L, "ACC001", EventType.DEBIT, new BigDecimal("2000.00"), "USD", testDateTime, EventStatus.PENDING
        );

        // When
        ResponseEntity<String> response = accountController.createTransaction(debitRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(accountService, times(1)).processTransaction(any(EventRequest.class), eq("ACC001"));
    }

    @Test
    @DisplayName("When: Account has very high balance, Then: Balance is correctly retrieved")
    void testGetHighBalance() {
        // Given
        Account highBalanceAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(new BigDecimal("999999.99"))
                .build();

        when(accountService.findByAccountId("ACC001")).thenReturn(highBalanceAccount);

        // When
        ResponseEntity<BigDecimal> response = accountController.getBalance("ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("999999.99"), response.getBody());
    }

    @Test
    @DisplayName("When: Account with zero balance is retrieved, Then: Zero balance is returned")
    void testGetZeroBalance() {
        // Given
        Account zeroBalanceAccount = Account.builder()
                .id(1L)
                .accountId("ACC001")
                .balance(BigDecimal.ZERO)
                .build();

        when(accountService.findByAccountId("ACC001")).thenReturn(zeroBalanceAccount);

        // When
        ResponseEntity<BigDecimal> response = accountController.getBalance("ACC001");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(BigDecimal.ZERO, response.getBody());
    }

    @Test
    @DisplayName("When: Null pointer exception is thrown from service, Then: Bad request is returned")
    void testCreateTransactionNullPointerException() {
        // Given
        when(accountService.processTransaction(validEventRequest, "ACC001"))
                .thenThrow(new NullPointerException("Cannot process. accountId is null"));

        // When
        ResponseEntity<String> response = accountController.createTransaction(validEventRequest, "ACC001");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Cannot process"));
    }

    @Test
    @DisplayName("When: Multiple transactions are processed sequentially, Then: All are handled correctly")
    void testMultipleTransactions() {
        // Given
        when(accountService.processTransaction(any(EventRequest.class), eq("ACC001")))
                .thenReturn(ResponseEntity.ok("Transaction processed successfully"));

        EventRequest request1 = new EventRequest(1L, "ACC001", EventType.CREDIT, new BigDecimal("100.00"), "USD", testDateTime, EventStatus.PENDING);
        EventRequest request2 = new EventRequest(2L, "ACC001", EventType.CREDIT, new BigDecimal("200.00"), "USD", testDateTime, EventStatus.PENDING);

        // When
        ResponseEntity<String> response1 = accountController.createTransaction(request1, "ACC001");
        ResponseEntity<String> response2 = accountController.createTransaction(request2, "ACC001");

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(accountService, times(2)).processTransaction(any(EventRequest.class), eq("ACC001"));
    }
}


