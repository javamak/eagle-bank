package com.eaglebank.cbs.core_engine.service;

import com.eaglebank.cbs.core_engine.api.TransactionApiDelegate;
import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.entity.Transaction;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.CreateTransactionRequest;
import com.eaglebank.cbs.core_engine.model.ListTransactionsResponse;
import com.eaglebank.cbs.core_engine.model.TransactionResponse;
import com.eaglebank.cbs.core_engine.model.TransactionResponse.CurrencyEnum;
import com.eaglebank.cbs.core_engine.model.TransactionResponse.TypeEnum;
import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import com.eaglebank.cbs.core_engine.repository.TransactionRepository;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionApiDelegate {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ResponseEntity<TransactionResponse> createTransaction(
            String accountNumber, CreateTransactionRequest request) {

        User loggedInUser = getAuthenticatedUser();

        BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

        if (!account.getUserId().equals(loggedInUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to transact on this account");
        }

        double amount = request.getAmount();

        if (request.getType() == CreateTransactionRequest.TypeEnum.WITHDRAWAL) {
            if (account.getBalance() < amount) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
            }
            account.setBalance(account.getBalance() - amount);
        } else {
            account.setBalance(account.getBalance() + amount);
        }
        try {
            // Flush now so concurrent balance updates fail inside this request.
            bankAccountRepository.saveAndFlush(account);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account balance was updated by another request. Please retry.");
        }

        String id = "tan-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAccountNumber(accountNumber);
        transaction.setUserId(loggedInUser.getId());
        transaction.setAmount(amount);
        transaction.setCurrency(request.getCurrency().getValue());
        transaction.setType(request.getType().getValue());
        transaction.setReference(request.getReference());

        Transaction saved = transactionRepository.save(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTransactionResponse(saved));
    }

    @Override
    public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(
            String accountNumber, String transactionId) {

        User loggedInUser = getAuthenticatedUser();

        BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

        if (!account.getUserId().equals(loggedInUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account's transactions");
        }

        Transaction transaction = transactionRepository.findByIdAndAccountNumberAndDeletedFalse(transactionId, accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    @Override
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {

        User loggedInUser = getAuthenticatedUser();

        BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

        if (!account.getUserId().equals(loggedInUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account's transactions");
        }

        List<TransactionResponse> transactions = transactionRepository
                .findByAccountNumberAndDeletedFalse(accountNumber)
                .stream()
                .map(this::toTransactionResponse)
                .toList();

        return ResponseEntity.ok(new ListTransactionsResponse(transactions));
    }

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                CurrencyEnum.fromValue(transaction.getCurrency()),
                TypeEnum.fromValue(transaction.getType()),
                toOffsetDateTime(transaction.getCreatedTimestamp()));
        response.setReference(transaction.getReference());
        return response;
    }

    private OffsetDateTime toOffsetDateTime(Instant timestamp) {
        return timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC);
    }
}
