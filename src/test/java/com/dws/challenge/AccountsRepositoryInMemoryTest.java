package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountsRepositoryInMemoryTest {

    @InjectMocks
    private AccountsRepositoryInMemory accountsRepository;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // Tests for Other methods

    @RepeatedTest(10)
    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    void shouldTransferMoneyConcurrently() throws InterruptedException {
        // Create two mock accounts with initial balances
        Account accountFrom = new Account("accountFrom");
        accountFrom.setBalance(new BigDecimal("2000.00"));
        Account accountTo = new Account("accountTo");
        accountTo.setBalance(new BigDecimal("1000.00"));

        // Initialize accounts in the repository
        accountsRepository.clearAccounts();
        accountsRepository.createAccount(accountFrom);
        accountsRepository.createAccount(accountTo);

        int numberOfThreads = 1000;
        BigDecimal transferAmount = new BigDecimal(1);

        // CountDownLatches for synchronization
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CountDownLatch latch1 = new CountDownLatch(numberOfThreads);
        CountDownLatch latch2 = new CountDownLatch(numberOfThreads);

        // ExecutorServices for concurrent transfers
        ExecutorService executorServiceFromTo = Executors.newFixedThreadPool(numberOfThreads);
        ExecutorService executorServiceToFrom = Executors.newFixedThreadPool(numberOfThreads);
        ExecutorService executorServiceFromToAgain = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            // Concurrent transfer accountFrom -> accountTo
            executorServiceFromTo.submit(() -> {
                try {
                    accountsRepository.transferMoney("accountFrom", "accountTo", transferAmount);
                } catch (Exception e) {
                    // Handle exceptions if needed
                } finally {
                    latch.countDown();
                }
            });

            // Concurrent transfer accountTo -> accountFrom
            executorServiceToFrom.submit(() -> {
                try {
                    accountsRepository.transferMoney("accountTo", "accountFrom", transferAmount);
                } catch (Exception e) {
                    // Handle exceptions if needed
                } finally {
                    latch1.countDown();
                }
            });

            // Concurrent transfer accountFrom -> accountTo again
            executorServiceFromToAgain.submit(() -> {
                try {
                    accountsRepository.transferMoney("accountFrom", "accountTo", transferAmount);
                } catch (Exception e) {
                    // Handle exceptions if needed
                } finally {
                    latch2.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await();
        latch1.await();
        latch2.await();

        // Verify that the accounts were updated correctly
        BigDecimal expectedAccountFromBalance = new BigDecimal("1000.00");
        BigDecimal expectedAccountToBalance = new BigDecimal("2000.00");
        assertEquals(expectedAccountFromBalance, accountFrom.getBalance());
        assertEquals(expectedAccountToBalance, accountTo.getBalance());
    }
}
