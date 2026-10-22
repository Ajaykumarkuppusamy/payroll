package com.payroll.aj.common.enums;

/**
 * ISO 4217 currency codes supported by the payroll platform.
 *
 * <p>Each entry carries its display symbol and the number of decimal
 * places used for monetary rounding in that currency. The payroll
 * engine references {@link #getDecimalPlaces()} when setting
 * {@link java.math.BigDecimal} scale with
 * {@link java.math.RoundingMode#HALF_UP}.</p>
 */
public enum CurrencyCode {

    USD("US Dollar",            "$",    2),
    EUR("Euro",                 "€",    2),
    GBP("British Pound",        "£",    2),
    INR("Indian Rupee",         "₹",    2),
    AED("UAE Dirham",           "د.إ",  2),
    SGD("Singapore Dollar",     "S$",   2),
    AUD("Australian Dollar",    "A$",   2),
    CAD("Canadian Dollar",      "C$",   2),
    JPY("Japanese Yen",         "¥",    0),
    CHF("Swiss Franc",          "CHF",  2),
    SAR("Saudi Riyal",          "﷼",    2),
    MYR("Malaysian Ringgit",    "RM",   2),
    ZAR("South African Rand",   "R",    2),
    BRL("Brazilian Real",       "R$",   2),
    CNY("Chinese Yuan",         "¥",    2);

    private final String displayName;
    private final String symbol;
    private final int decimalPlaces;

    CurrencyCode(String displayName, String symbol, int decimalPlaces) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
