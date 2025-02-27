package com.db.awmd.challenge.web;

import static com.db.awmd.challenge.constant.MessageConstants.*;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AmountTransfer;
import com.db.awmd.challenge.exception.AmountTransferException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
@Scope("request")
public class AccountsController {

	private final AccountsService accountsService;
	private final NotificationService notificationService;

	@Autowired
	public AccountsController(AccountsService accountsService, NotificationService notificationService) {
		this.accountsService = accountsService;
		this.notificationService = notificationService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
		log.info("Creating account {}", account);

		try {
			this.accountsService.createAccount(account);
		} catch (DuplicateAccountIdException daie) {
			return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(String.format(ACCOUNT_CREATED, account.getAccountId()), HttpStatus.CREATED);
	}

	@GetMapping(path = "/{accountId}")
	public ResponseEntity<Object> getAccount(@PathVariable String accountId) {
		log.info("Retrieving account for id {}", accountId);
		Account account = this.accountsService.getAccount(accountId);
		if (account == null) {
			return new ResponseEntity<>(String.format(ACCOUNT_DOES_NOT_EXIST, accountId), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(account, HttpStatus.OK);
	}

	@PutMapping(path = "/block/{accountId}")
	public ResponseEntity<Object> blockAccount(@PathVariable String accountId) {
		log.info("Blocking account for id {}", accountId);
		try {
			this.accountsService.blockAccount(accountId);
		} catch (Exception e) {
			return new ResponseEntity<>(String.format(ACCOUNT_BLOCKED_UNSUCCESSFUL, accountId) + "\n" + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(String.format(ACCOUNT_BLOCKED_SUCCESSFUL, accountId), HttpStatus.OK);
	}

	@PutMapping(path = "/unblock/{accountId}")
	public ResponseEntity<Object> unblockAccount(@PathVariable String accountId) {
		log.info("Unblocking account for id {}", accountId);
		try {
			this.accountsService.unblockAccount(accountId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					String.format(ACCOUNT_UNBLOCKED_UNSUCCESSFUL, accountId) + "\n" + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(String.format(ACCOUNT_UNBLOCKED_SUCCESSFUL, accountId), HttpStatus.OK);
	}

	@PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> amountTransfer(@RequestBody @Valid AmountTransfer amountTransfer) {
		try {
			
			this.accountsService.amountTransfer(amountTransfer.getAccountFrom(), amountTransfer.getAccountTo(),
					amountTransfer.getTransferAmount());

			Account fromAccount = accountsService.getAccount(amountTransfer.getAccountFrom());
			Account toAccount = accountsService.getAccount(amountTransfer.getAccountTo());

			String fromAccountDescr = String.format(FROM_ACCOUNT_NOTIFICATION, amountTransfer.getTransferAmount(),
					toAccount.getAccountId());
			String toAccountDescr = String.format(TO_ACCOUNT_NOTIFICATION, amountTransfer.getTransferAmount(),
					fromAccount.getAccountId());

			notificationService.notifyAboutTransfer(fromAccount, fromAccountDescr);
			notificationService.notifyAboutTransfer(toAccount, toAccountDescr);

		} catch (AmountTransferException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>("Transfer Completed", HttpStatus.ACCEPTED);
	}

}
