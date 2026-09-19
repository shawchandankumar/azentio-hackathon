package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A sanctioned or high-risk jurisdiction (Business Rule 4). Configurable at runtime. */
@Entity
@Table(name = "high_risk_jurisdictions")
@Getter
@Setter
@NoArgsConstructor
public class HighRiskJurisdiction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, unique = true, length = 2)
    private String countryCode;

    @Column(name = "country_name", nullable = false, length = 120)
    private String countryName;

    /** SANCTIONED or HIGH_RISK. */
    @Column(nullable = false, length = 20)
    private String category = "HIGH_RISK";

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 300)
    private String notes;
}
