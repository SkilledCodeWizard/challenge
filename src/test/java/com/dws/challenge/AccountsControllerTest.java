package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.MoneyTransferService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    void transferMoney() throws Exception {
        String uniqueAccountIdFrom = "Id-" + System.currentTimeMillis() + "from";
        String uniqueAccountIdTo = "Id-" + System.currentTimeMillis() + "to";
        // Create two accounts with initial balances
        Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("100.00"));
        Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("50.00"));

        // Add accounts to the repository
        accountsService.getAccountsRepository().createAccount(accountFrom);
        accountsService.getAccountsRepository().createAccount(accountTo);

        // Perform money transfer
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFrom\":\"" + uniqueAccountIdFrom + "\",\"accountTo\":\"" + uniqueAccountIdTo + "\",\"amount\":20.00}"))
                .andExpect(status().isOk());

        // Check account balances after transfer
        Account updatedAccount1 = accountsService.getAccount(uniqueAccountIdFrom);
        Account updatedAccount2 = accountsService.getAccount(uniqueAccountIdTo);

        assertThat(updatedAccount1.getBalance()).isEqualByComparingTo("80.00");
        assertThat(updatedAccount2.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void transferMoneyInvalidAmount() throws Exception {
        String uniqueAccountIdFrom = "Id-" + System.currentTimeMillis() + "from";
        String uniqueAccountIdTo = "Id-" + System.currentTimeMillis() + "to";

        // Attempt to transfer an invalid amount (negative amount)
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFrom\":\"" + uniqueAccountIdFrom + "\",\"accountTo\":\"" + uniqueAccountIdTo + "\",\"amount\":-20.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferMoneyInsufficientBalance() throws Exception {
        String uniqueAccountIdFrom = "Id-" + System.currentTimeMillis() + "from";
        String uniqueAccountIdTo = "Id-" + System.currentTimeMillis() + "to";

        // Create two accounts with initial balances
        Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("50.00"));
        Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("100.00"));

        // Add accounts to the repository
        accountsService.getAccountsRepository().createAccount(accountFrom);
        accountsService.getAccountsRepository().createAccount(accountTo);

        // Attempt to transfer an amount exceeding the balance of account1
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFrom\":\"" + uniqueAccountIdFrom + "\",\"accountTo\":\"" + uniqueAccountIdTo + "\",\"amount\":60.00}"))
                .andExpect(status().isBadRequest());
    }

}

