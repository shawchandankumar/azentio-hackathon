package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A watchlisted / sanctioned counterparty name (Business Rule 4). */
@Entity
@Table(name = "watchlist_counterparties")
@Getter
@Setter
@NoArgsConstructor
public class WatchlistCounterparty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** SANCTIONED or HIGH_RISK. */
    @Column(nullable = false, length = 20)
    private String category = "SANCTIONED";

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 300)
    private String notes;
}
