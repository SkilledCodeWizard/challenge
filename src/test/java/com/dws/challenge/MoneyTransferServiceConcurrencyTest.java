package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.MoneyTransferService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MoneyTransferServiceConcurrencyTest {

    @Autowired
    private MoneyTransferService moneyTransferService;

    @RepeatedTest(5)
    @Timeout(value = 10)
    void shouldTransferMoneyConcurrently() throws InterruptedException {
        Account accountFrom = new Account("accountFrom");
        Account accountTo = new Account("accountTo");

        // Create two mock accounts with initial balances
        accountFrom.setBalance(new BigDecimal("2000.00"));
        accountTo.setBalance(new BigDecimal("1000.00"));

        AccountsRepository accountsRepository = moneyTransferService.getAccountsRepository();

        // Initialize accounts in the repository
        accountsRepository.clearAccounts();
        accountsRepository.createAccount(accountFrom);
        accountsRepository.createAccount(accountTo);

        System.out.println("Initialised");

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
                    moneyTransferService.transferMoney("accountFrom", "accountTo", transferAmount);
                } catch (Exception e) {
                    // Handle exceptions if needed
                } finally {
                    latch.countDown();
                }
            });

            // Concurrent transfer accountTo -> accountFrom
            executorServiceToFrom.submit(() -> {
                try {
                    moneyTransferService.transferMoney("accountTo", "accountFrom", transferAmount);
                } catch (Exception e) {
                    // Handle exceptions if needed
                } finally {
                    latch1.countDown();
                }
            });

            // Concurrent transfer accountFrom -> accountTo again
            executorServiceFromToAgain.submit(() -> {
                try {
                    moneyTransferService.transferMoney("accountFrom", "accountTo", transferAmount);
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