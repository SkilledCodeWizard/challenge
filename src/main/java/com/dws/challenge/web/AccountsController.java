package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.MoneyTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountNotFoundException;
import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;
  private final MoneyTransferService moneyTransferService;

  @Autowired
  public AccountsController(AccountsService accountsService, MoneyTransferService moneyTransferService) {
    this.accountsService = accountsService;
    this.moneyTransferService = moneyTransferService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferMoney(@RequestBody @Valid TransferRequest transferRequest) {
    log.info("Transferring money: {}", transferRequest);

    try {
      moneyTransferService.transferMoney(
              transferRequest.getAccountFrom(),
              transferRequest.getAccountTo(),
              transferRequest.getAmount()
      );
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (AccountNotFoundException | InvalidTransferAmountException | InsufficientBalanceException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }
}
