package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import org.springframework.stereotype.Repository;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        lock.writeLock().lock();
        try {
            Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
            if (previousAccount != null) {
                throw new DuplicateAccountIdException(
                        "Account id " + account.getAccountId() + " already exists!");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Account getAccount(String accountId) {
        Account result = null;
        lock.readLock().lock();
        try {
            result = accounts.get(accountId);
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void clearAccounts() {
        lock.writeLock().lock();
        try {
            accounts.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void transferMoney(String accountFromId, String accountToId, BigDecimal amount)
            throws AccountNotFoundException, InsufficientBalanceException, InvalidTransferAmountException {
        lock.writeLock().lock();
        try {
            Account accountFrom = getAccount(accountFromId);
            Account accountTo = getAccount(accountToId);

            if (accountFrom == null || accountTo == null) {
                throw new AccountNotFoundException("One or both accounts not found");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransferAmountException("Invalid transfer amount");
            }

            BigDecimal accountFromBalance = accountFrom.getBalance();
            if (accountFromBalance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in account " + accountFromId);
            }

            // Perform the transfer within the repository
            accountFrom.withdraw(amount);
            accountTo.deposit(amount);

        } finally {
            lock.writeLock().unlock();
        }
    }
}
