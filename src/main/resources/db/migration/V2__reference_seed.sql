-- ============================================================================
-- Sentinel AML — Reference & configuration seed data
-- Rule thresholds live here so compliance can tune them without a redeploy.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Exchange rates (1 unit of currency = rate_to_base INR). Base currency = INR.
-- ---------------------------------------------------------------------------
INSERT INTO exchange_rates (currency_code, rate_to_base) VALUES
    ('INR', 1.000000),
    ('USD', 83.200000),
    ('EUR', 90.100000),
    ('GBP', 105.400000),
    ('AED', 22.650000),
    ('SGD', 61.800000),
    ('CHF', 94.300000),
    ('JPY', 0.560000),
    ('RUB', 0.910000),
    ('AUD', 55.200000);

-- ---------------------------------------------------------------------------
-- High-risk / sanctioned jurisdictions (illustrative, configurable)
-- ---------------------------------------------------------------------------
INSERT INTO high_risk_jurisdictions (country_code, country_name, category, notes) VALUES
    ('IR', 'Iran',                    'SANCTIONED', 'FATF call for action'),
    ('KP', 'North Korea',             'SANCTIONED', 'FATF call for action'),
    ('MM', 'Myanmar',                 'HIGH_RISK',  'FATF increased monitoring'),
    ('SY', 'Syria',                   'SANCTIONED', 'Comprehensive sanctions'),
    ('AF', 'Afghanistan',             'HIGH_RISK',  'Elevated ML/TF risk'),
    ('YE', 'Yemen',                   'HIGH_RISK',  'FATF increased monitoring'),
    ('PA', 'Panama',                  'HIGH_RISK',  'Offshore secrecy concerns'),
    ('KY', 'Cayman Islands',          'HIGH_RISK',  'Offshore secrecy concerns');

-- ---------------------------------------------------------------------------
-- Watchlisted counterparties (sanctions/adverse-media hits)
-- ---------------------------------------------------------------------------
INSERT INTO watchlist_counterparties (name, country_code, category, notes) VALUES
    ('Nordwind Holdings Ltd',   'KY', 'SANCTIONED', 'Shell entity - adverse media'),
    ('Zarubezh Import Export',  'RU', 'SANCTIONED', 'OFAC SDN list'),
    ('Crescent Trading FZE',    'IR', 'SANCTIONED', 'Sanctions evasion network');

-- ---------------------------------------------------------------------------
-- Detection rule configuration. params_json holds tunable thresholds/windows.
-- ---------------------------------------------------------------------------
INSERT INTO rule_config (rule_code, rule_type, display_name, description, enabled, base_weight, severity, params_json) VALUES
    ('R1_LARGE_TXN', 'LARGE_TRANSACTION',
     'Large Transaction (CTR)',
     'Any single transaction at or above the reporting threshold.',
     true, 60, 'MEDIUM',
     '{"thresholdBase": 831960.00}'),

    ('R2_STRUCTURING', 'STRUCTURING',
     'Structuring / Smurfing',
     'Multiple transactions just below the reporting threshold within a rolling window.',
     true, 80, 'HIGH',
     '{"windowHours": 24, "minCount": 3, "lowerBase": 748764.00, "upperBase": 831876.00}'),

    ('R3_RAPID_MOVEMENT', 'RAPID_MOVEMENT',
     'Rapid Movement of Funds',
     'Large credit followed by outbound transfer of most of the value within a short window (layering).',
     true, 85, 'HIGH',
     '{"windowHours": 48, "outflowRatio": 0.80, "minCreditBase": 415980.00}'),

    ('R4_HIGH_RISK_JURISDICTION', 'HIGH_RISK_JURISDICTION',
     'High-Risk Jurisdiction / Sanctions',
     'Transaction to/from a sanctioned or high-risk jurisdiction or watchlisted counterparty.',
     true, 90, 'CRITICAL',
     '{}'),

    ('R5_BEHAVIORAL_DEVIATION', 'BEHAVIORAL_DEVIATION',
     'Behavioral Deviation',
     'Daily transaction value exceeds a multiple of the customer 90-day rolling average.',
     true, 70, 'HIGH',
     '{"lookbackDays": 90, "multiplier": 3.0, "minBaselineBase": 41598.00}'),

    ('R6_ROUND_NUMBER', 'ROUND_NUMBER',
     'Round-Number Pattern',
     'Repeated suspiciously round-value transactions within a rolling window.',
     true, 40, 'LOW',
     '{"windowHours": 72, "minCount": 3, "roundUnit": 1000}');
