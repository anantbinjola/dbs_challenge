package com.db.awmd.challenge;

import static com.db.awmd.challenge.constant.MessageConstants.ACCOUNT_DOES_NOT_EXIST;
import static com.db.awmd.challenge.constant.MessageConstants.ACCOUNT_IS_BLOCKED;
import static com.db.awmd.challenge.constant.MessageConstants.INSUFFICIENT_BALANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void amountTransfer_TransactionCommit() throws Exception {
		Account accountFrom = new Account("Id-341");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);
		Account accountTo = new Account("Id-342");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);
		this.accountsService.amountTransfer("Id-341", "Id-342", new BigDecimal(1000));
		assertThat(this.accountsService.getAccount("Id-341").getBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(this.accountsService.getAccount("Id-342").getBalance()).isEqualTo(new BigDecimal(2000));

	}

	@Test
	public void amountTransfer_TransactionRollBack() throws Exception {
		Account accountFrom = new Account("Id-350");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);
		Account accountTo = new Account("Id-351");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);
		this.accountsService.amountTransfer("Id-350", "Id-351", new BigDecimal(1000));

		try {
			// make transfer when balance insufficient
			this.accountsService.amountTransfer("Id-350", "Id-351", new BigDecimal(500));
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(INSUFFICIENT_BALANCE, "Id-350"));
		}
		// Transaction will be rollBack and no account will be updated
		assertThat(this.accountsService.getAccount("Id-350").getBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(this.accountsService.getAccount("Id-351").getBalance()).isEqualTo(new BigDecimal(2000));

	}

	@Test
	public void amountTransfer_TransactionRollBackOnNonExistingToAccount() throws Exception {
		// make transfer To an Account which do not exist
		Account accountFrom = new Account("Id-360");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);
		try {
			this.accountsService.amountTransfer("Id-360", "Id-361", new BigDecimal(500));
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_DOES_NOT_EXIST, "Id-361"));
		}
		// Transaction will be rollBack and no debit will happen
		assertThat(this.accountsService.getAccount("Id-360").getBalance()).isEqualTo(new BigDecimal(1000));
	}

	// adding test cases to increase coverage
	// UnitTest to test case when from account does not exist.
	@Test
	public void amountTransfer_TransactionRollBackOnNonExistingFromAccount() throws Exception {
		// make transfer To an Account which do not exist
		Account accountFrom = new Account("Id-371");
		accountFrom.setBalance(new BigDecimal(1000));

		Account accountTo = new Account("Id-372");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);

		try {
			this.accountsService.amountTransfer("Id-371", "Id-372", new BigDecimal(500));
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_DOES_NOT_EXIST, "Id-371"));
		}
		// Transaction will be rollBack and no debit will happen
		assertThat(this.accountsService.getAccount("Id-372").getBalance()).isEqualTo(new BigDecimal(1000));
	}

	// Test case for blocking an account from transaction
	@Test
	public void blockAccount_TransactionCommit() throws Exception {
		Account account = new Account("Id-381");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);
		this.accountsService.blockAccount(account.getAccountId());
		Account expected = this.accountsService.getAccount("Id-381");
		assertTrue(expected.isBlocked());
	}

	@Test
	public void blockAccount_TransactionCommitOnNonExistingAccount() throws Exception {
		Account account = new Account("Id-391");
		account.setBalance(new BigDecimal(1000));
		try {
			this.accountsService.blockAccount(account.getAccountId());
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_DOES_NOT_EXIST, "Id-391"));
		}
	}

	// Test case for unblocking an account from transaction
	@Test
	public void unblockAccount_TransactionCommit() throws Exception {
		Account account = new Account("Id-400");
		account.setBalance(new BigDecimal(1000));
		account.setBlocked(true);
		this.accountsService.createAccount(account);
		this.accountsService.unblockAccount(account.getAccountId());
		Account expected = this.accountsService.getAccount("Id-400");
		assertFalse(expected.isBlocked());
	}

	@Test
	public void unblockAccount_TransactionCommitOnNonExistingAccount() throws Exception {
		Account account = new Account("Id-410");
		account.setBalance(new BigDecimal(1000));
		account.setBlocked(true);
		try {
			this.accountsService.unblockAccount(account.getAccountId());
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_DOES_NOT_EXIST, "Id-410"));
		}

	}

	@Test
	public void amountTransfer_TransactionRollbackOnToAccountBlocked() throws Exception {
		Account accountFrom = new Account("Id-420");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);
		Account accountTo = new Account("Id-421");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);
		this.accountsService.blockAccount("Id-421");
		try {
			this.accountsService.amountTransfer("Id-420", "Id-421", new BigDecimal(1000));
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_IS_BLOCKED, "Id-421"));
		}

		assertThat(this.accountsService.getAccount("Id-420").getBalance()).isEqualTo(new BigDecimal(1000));
		assertThat(this.accountsService.getAccount("Id-421").getBalance()).isEqualTo(new BigDecimal(1000));

	}

	@Test
	public void amountTransfer_TransactionRollbackOnFromAccountBlocked() throws Exception {
		Account accountFrom = new Account("Id-430");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);
		Account accountTo = new Account("Id-431");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);
		this.accountsService.blockAccount("Id-430");
		try {
			this.accountsService.amountTransfer("Id-430", "Id-431", new BigDecimal(1000));
		} catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo(String.format(ACCOUNT_IS_BLOCKED, "Id-430"));
		}

		assertThat(this.accountsService.getAccount("Id-430").getBalance()).isEqualTo(new BigDecimal(1000));
		assertThat(this.accountsService.getAccount("Id-431").getBalance()).isEqualTo(new BigDecimal(1000));

	}

}
