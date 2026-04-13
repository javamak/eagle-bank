package com.eaglebank.cbs.core_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE transactions SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Data
@NoArgsConstructor
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_account_number", columnList = "accountNumber"),
        @Index(name = "idx_transactions_user_id", columnList = "userId")
    }
)
public class Transaction {

    @Id
    private String id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String type;

    @Column
    private String reference;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdTimestamp;

    @Column(nullable = false)
    private boolean deleted = false;

}
