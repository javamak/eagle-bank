package com.eaglebank.cbs.core_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@Table(
    name = "bank_accounts",
    indexes = {
        @Index(name = "idx_bank_accounts_user_id", columnList = "userId")
    }
)
public class BankAccount {

    @Id
    private String accountNumber;

    @Column(nullable = false)
    private String sortCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private Double balance;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String userId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdTimestamp;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedTimestamp;

    @Version
    private Long version;

    @Column(nullable = false)
    private boolean deleted = false;

}
