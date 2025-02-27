package com.db.awmd.challenge;

import static com.db.awmd.challenge.constant.MessageConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;
  
  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45,\"blocked\":false}"));
  }
  
  //Added Test case for handling case where account id is not found.
  //Returning 404 not found status
  @Test
  public void getAccount_NonExistent() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isNotFound());
  }
  
  @Test
  public void amountTransfer() throws Exception {
	  	String accountIdFrom = "Id-360";
	    Account accountFrom = new Account(accountIdFrom, new BigDecimal("123.45"));
	    this.accountsService.createAccount(accountFrom);
	    String accountIdTo = "Id-361";
	    Account accountTo = new Account(accountIdTo, new BigDecimal("123.45"));
	    this.accountsService.createAccount(accountTo);
	    
	    this.mockMvc.perform(post("/v1/accounts/transfer/").contentType(MediaType.APPLICATION_JSON)
	    	      .content("{\"accountFrom\":\"Id-360\",\"accountTo\":\"Id-361\",\"transferAmount\":100}")).andExpect(status().isAccepted());
  }
  
  @Test
  public void amountTransfer_InvalidAmount() throws Exception {
	  	String accountIdFrom = "Id-360";
	    Account accountFrom = new Account(accountIdFrom, new BigDecimal("123.45"));
	    this.accountsService.createAccount(accountFrom);
	    String accountIdTo = "Id-361";
	    Account accountTo = new Account(accountIdTo, new BigDecimal("123.45"));
	    this.accountsService.createAccount(accountTo);
	    
	    this.mockMvc.perform(post("/v1/accounts/transfer/").contentType(MediaType.APPLICATION_JSON)
	    	      .content("{\"accountFrom\":\"Id-360\",\"accountTo\":\"Id-361\",\"transferAmount\":-100}")).andExpect(status().isBadRequest());
  }
  
  @Test
  public void blockAccount() throws Exception {
	  	String accountId = "Id-362";
	    Account account = new Account(accountId, new BigDecimal("123.45"));
	    this.accountsService.createAccount(account);
	    this.mockMvc.perform(put("/v1/accounts/block/"+accountId).contentType(MediaType.APPLICATION_JSON))
	    		.andExpect(status().isOk()).andExpect(content().string(String.format(ACCOUNT_BLOCKED_SUCCESSFUL, accountId)));
  }
  
  @Test
  public void blockAccount_NonExistingAccount() throws Exception {
	  	String accountId = "Id-363";
	    this.mockMvc.perform(put("/v1/accounts/block/"+accountId).contentType(MediaType.APPLICATION_JSON))
	    	      .andExpect(status().isInternalServerError()).andExpect(content().string(String.format(ACCOUNT_BLOCKED_UNSUCCESSFUL+"\n"+ACCOUNT_DOES_NOT_EXIST, accountId,accountId)));
  }
  
  @Test
  public void unblockAccount() throws Exception {
	  	String accountId = "Id-364";
	    Account account = new Account(accountId, new BigDecimal("123.45"),true);
	    this.accountsService.createAccount(account);
	    this.mockMvc.perform(put("/v1/accounts/unblock/"+accountId).contentType(MediaType.APPLICATION_JSON)
	    	      .content(String.format(ACCOUNT_UNBLOCKED_SUCCESSFUL, accountId))).andExpect(status().isOk());
  }
  
  @Test
  public void unblockAccount_NonExistingAccount() throws Exception {
	  	String accountId = "Id-365";
	    this.mockMvc.perform(put("/v1/accounts/unblock/"+accountId).contentType(MediaType.APPLICATION_JSON))
	    .andExpect(status().isInternalServerError()).andExpect(content().string(String.format(ACCOUNT_UNBLOCKED_UNSUCCESSFUL+"\n"+ACCOUNT_DOES_NOT_EXIST, accountId,accountId)));
  }
  
}
