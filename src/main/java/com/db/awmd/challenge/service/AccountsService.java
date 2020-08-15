package com.db.awmd.challenge.service;

import static com.db.awmd.challenge.constant.MessageConstants.ACCOUNT_DOES_NOT_EXIST;
import static com.db.awmd.challenge.constant.MessageConstants.ACCOUNT_IS_BLOCKED;
import static com.db.awmd.challenge.constant.MessageConstants.INSUFFICIENT_BALANCE;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AmountTransferException;
import com.db.awmd.challenge.exception.StatusChangeException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.transaction.AccountTransactionManager;

import lombok.Getter;

@Service
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	private AccountTransactionManager transactionManager;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository) {
		this.accountsRepository = accountsRepository;
		this.transactionManager = new AccountTransactionManager(accountsRepository);
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public boolean blockAccount(final String accountId) throws AmountTransferException {
		transactionManager.doInTransaction(() -> {
			this.setAccountBlockedStatus(accountId, true);
		});
		transactionManager.commit();
		return true;
	}

	public boolean unblockAccount(String accountId) throws AmountTransferException {
		transactionManager.doInTransaction(() -> {
			this.setAccountBlockedStatus(accountId, false);
		});
		transactionManager.commit();
		return true;
	}

	// @Transactional(propagation=Propagation.REQUIRED, readOnly=false,
	// rollbackFor=AmountTransferException.class)
	public void amountTransfer(final String fromAccount, final String toAccount, final BigDecimal transferAmount)
			throws AmountTransferException {

		transactionManager.doInTransaction(() -> {

			this.debit(fromAccount, transferAmount);
			this.credit(toAccount, transferAmount);
		});
		transactionManager.commit();

	}

	private Account debit(String accountId, BigDecimal amount) throws AmountTransferException {
		// take repository from transaction manager in order to manage transactions and
		// rollBack.
		// But, This method will only be transactional only if this is called within
		// "transactionManager.doInTransaction()
		// OR method annotated with @AccountTransaction.
		final Account account = transactionManager.getRepoProxy().getAccount(accountId);
		if (account == null) {
			throw new AmountTransferException(String.format(ACCOUNT_DOES_NOT_EXIST, accountId));
		}
		if (account.getBalance().compareTo(amount) == -1) {
			throw new AmountTransferException(String.format(INSUFFICIENT_BALANCE, accountId));
		}
		if (account.isBlocked()) {
			throw new AmountTransferException(String.format(ACCOUNT_IS_BLOCKED, accountId));
		}
		BigDecimal bal = account.getBalance().subtract(amount);
		account.setBalance(bal);
		return account;
	}

	private Account credit(String accountId, BigDecimal amount) throws AmountTransferException {
		// take repository from transaction manager in order to manage transactions and
		// rollBack.
		// But, This method will only be transactional only if this is called within
		// "transactionManager.doInTransaction()
		// OR method annotated with @AccountTransaction.
		final Account account = transactionManager.getRepoProxy().getAccount(accountId);
		if (account == null) {
			throw new AmountTransferException(String.format(ACCOUNT_DOES_NOT_EXIST, accountId));
		}
		if (account.isBlocked()) {
			throw new AmountTransferException(String.format(ACCOUNT_IS_BLOCKED, accountId));
		}
		BigDecimal bal = account.getBalance().add(amount);
		account.setBalance(bal);
		return account;
	}

	private Account setAccountBlockedStatus(String accountId, boolean status) throws StatusChangeException {
		// take repository from transaction manager in order to manage transactions and
		// rollBack.
		// But, This method will only be transactional only if this is called within
		// "transactionManager.doInTransaction()
		// OR method annotated with @AccountTransaction.
		final Account account = transactionManager.getRepoProxy().getAccount(accountId);
		if (account == null) {
			throw new StatusChangeException(String.format(ACCOUNT_DOES_NOT_EXIST, accountId));
		}
		account.setBlocked(status);
		return account;
	}
}
