package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import com.dws.challenge.repository.AccountsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;

@Service
public class MoneyTransferService {

    private final AccountsRepository accountsRepository;
    private final NotificationService notificationService;

    @Autowired
    public MoneyTransferService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void transferMoney(String accountFromId, String accountToId, BigDecimal amount) throws AccountNotFoundException,
            InsufficientBalanceException, InvalidTransferAmountException {
        // Fetch both accounts from the repository
        Account accountFrom = accountsRepository.getAccount(accountFromId);
        Account accountTo = accountsRepository.getAccount(accountToId);

        if (accountFrom == null || accountTo == null) {
            throw new AccountNotFoundException("One or both accounts not found");
        }

        // Ensure that the amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException("Invalid transfer amount");
        }

        // Transfer money
        accountsRepository.transferMoney(accountFromId, accountToId, amount);

        // Notify both account holders
        String notificationMessage = "Amount " + amount + " transferred from account " + accountFromId;
        notificationService.notifyAboutTransfer(accountTo, notificationMessage);
        notificationMessage = "Amount " + amount + " received in account " + accountToId;
        notificationService.notifyAboutTransfer(accountFrom, notificationMessage);
    }
}