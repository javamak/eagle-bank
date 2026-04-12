package com.eaglebank.cbs.core_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(nullable = false)
    private String line1;

    private String line2;

    private String line3;

    @Column(nullable = false)
    private String town;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private String postcode;

}
