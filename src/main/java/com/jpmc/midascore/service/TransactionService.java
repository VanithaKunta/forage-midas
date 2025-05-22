package com.jpmc.midascore.service;


import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TransactionService {

    private final UserRepository userRepository;

    private final TransactionRecordRepository transactionRecordRepository;

    private final RestTemplate restTemplate;

    public TransactionService(UserRepository userRepository, TransactionRecordRepository transactionRecordRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = restTemplate;
    }

    public void processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        Float senderBalance = sender.getBalance();
        Float recipientBalance = recipient.getBalance();

        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setSender(sender);
            transactionRecord.setRecipient(recipient);
            transactionRecord.setAmount(transaction.getAmount());
            transactionRecordRepository.save(transactionRecord);
            String incentiveUrl = "http://localhost:8080/incentive";
            Incentive incentive = restTemplate.postForObject(incentiveUrl, transaction, Incentive.class);

            sender.setBalance(senderBalance - transaction.getAmount());
            recipient.setBalance(recipientBalance + transaction.getAmount() + incentive.getAmount());
            userRepository.save(sender);
            userRepository.save(recipient);
            System.out.println("Saved transaction record: " + transaction);
            System.out.println("Sender balance after transaction: " + sender);
            System.out.println("Recipient balance after transaction: " + recipient);

        } else {
            System.out.println("Transaction is not valid. Hence ignored: " + transaction);
        }
    }

    public Balance getBalance(long userId) {
        UserRecord user = userRepository.findById(userId);
        if(user != null) {
            return new Balance(user.getBalance());
        }
        return new Balance(0);
    }

}
