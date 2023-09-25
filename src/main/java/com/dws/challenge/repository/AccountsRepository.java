package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferAmountException;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Account getAccount(String accountId);

  void clearAccounts();

  void transferMoney(String accountFromId, String accountToId, BigDecimal amount) throws AccountNotFoundException,
          InsufficientBalanceException, InvalidTransferAmountException;
}
