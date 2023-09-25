package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.MoneyTransferService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MoneyTransferServiceTest {

    @InjectMocks
    private MoneyTransferService moneyTransferService;

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    public void testMoneyTransfer() throws AccountNotFoundException, InvalidTransferAmountException, InsufficientBalanceException {
        String uniqueAccountIdFrom = "Id-" + System.currentTimeMillis() + "from";
        String uniqueAccountIdTo = "Id-" + System.currentTimeMillis() + "to";

        // Create two accounts with initial balances
        Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("100.00"));
        Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("50.00"));

        // Mock the repository to return these accounts
        when(accountsRepository.getAccount(uniqueAccountIdFrom)).thenReturn(accountFrom);
        when(accountsRepository.getAccount(uniqueAccountIdTo)).thenReturn(accountTo);

        BigDecimal transferAmount = new BigDecimal("20.00");

        // Perform money transfer
        moneyTransferService.transferMoney(uniqueAccountIdFrom, uniqueAccountIdTo, transferAmount);

        verify(accountsRepository).transferMoney(accountFrom.getAccountId(), accountTo.getAccountId(), transferAmount);

        // Verify that notification service was called
        verify(notificationService, times(2)).notifyAboutTransfer(any(), anyString());
    }

    @Test
    public void testMoneyTransferInsufficientBalance() throws InvalidTransferAmountException, InsufficientBalanceException, AccountNotFoundException {
        String uniqueAccountIdFrom = "Id-" + System.currentTimeMillis() + "from";
        String uniqueAccountIdTo = "Id-" + System.currentTimeMillis() + "to";

        // Create two accounts with initial balances
        Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("50.00"));
        Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("100.00"));

        // Mock the repository to return these accounts
        when(accountsRepository.getAccount(uniqueAccountIdFrom)).thenReturn(accountFrom);
        when(accountsRepository.getAccount(uniqueAccountIdTo)).thenReturn(accountTo);

        BigDecimal transferAmount = new BigDecimal("60.00");

        doThrow(InsufficientBalanceException.class).when(accountsRepository).transferMoney(uniqueAccountIdFrom , uniqueAccountIdTo, transferAmount);

        // Attempt to transfer an amount exceeding the balance of account1
        assertThrows(InsufficientBalanceException.class,
                () -> moneyTransferService.transferMoney(uniqueAccountIdFrom, uniqueAccountIdTo, transferAmount));

        // Verify that neither account balance was updated
        assertEquals(new BigDecimal("50.00"), accountFrom.getBalance());
        assertEquals(new BigDecimal("100.00"), accountTo.getBalance());

        // Verify that notification service was not called
        verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
    }

}