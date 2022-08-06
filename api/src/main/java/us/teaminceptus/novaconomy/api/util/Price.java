package us.teaminceptus.novaconomy.api.util;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility Class for creating a Price with an Economy
 */
public final class Price implements ConfigurationSerializable {

    private Economy econ;
    private double amount;

    /**
     * Constructs a Price.
     * @param econ Economy to use
     * @param amount Price Amount
     * @throws IllegalArgumentException if amount is not positive
     */
    public Price(@Nullable Economy econ, double amount) throws IllegalArgumentException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        this.econ = econ;
        this.amount = amount;
    }

    /**
     * Constructs a Price from a Map Entry.
     * @param entry Map Entry
     */
    public Price(@NotNull Map.Entry<Economy, Double> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Fetches the Price amount.
     * @return Price Amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the current Price amount.
     * @param amount Price Amount
     * @return this Price, for chaining
     * @throws IllegalArgumentException if amount is not positive
     */
    @NotNull
    public Price setAmount(double amount) throws IllegalArgumentException {
       if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
       this.amount = amount;
       return this;
    }

    /**
     * Fetches the Price's Economy.
     * @return Price's Economy
     */
    @Nullable
    public Economy getEconomy() {
        return this.econ;
    }

    /**
     * Sets the current Price's Economy.
     * @param econ Price Economy
     * @return this Price, for chaining
     */
    @NotNull
    public Price setEconomy(@Nullable Economy econ) {
        this.econ = econ;
        return this;
    }

    /**
     * Converts this Price to another Price with Economy.
     * @param econ Economy to use
     * @return Converted Price with amount converted to Economy
     * @throws IllegalArgumentException if economy is null
     */
    @NotNull
    public Price convertTo(@NotNull Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        return new Price(econ, econ.convertAmount(this.econ, this.amount));
    }

    /**
     * Adds an amount to this Price's amount.
     * @param amount Amount to add
     * @return this Price, for chaining
     */
    @NotNull
    public Price add(double amount) {
        this.amount += amount;
        return this;
    }

    /**
     * Adds a Price's amount to this Price's amount.
     * @param price Price to add
     * @return this Price, for chaining
     */
    @NotNull
    public Price add(@Nullable Price price) {
        if (price == null) return this;
        return add(price.amount);
    }

    /**
     * Removes a Price's amount from this Price's amount.
     * @param price Price to remove
     * @return this Price, for chaining
     */
    @NotNull
    public Price remove(@Nullable Price price) {
        if (price == null) return this;
        return remove(price.amount);
    }

    /**
     * Fetches the Price's Symbol.
     * @return Price Symbol
     */
    public char getPriceSymbol() { return this.econ.getSymbol(); }

    /**
     * Removes this amount from this Price's current amount.
     * @param amount Amount to remove
     * @return this Price, for chaining
     */
    @NotNull
    public Price remove(double amount) {
        this.amount -= amount;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("amount", amount);
            put("economy", econ.getName().toLowerCase());
        }};
    }

    /**
     * Deserializes a Map into a Price.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized Price
     * @throws IllegalArgumentException if an argument is malformed or missing
     */
    @Nullable
    public static Price deserialize(@Nullable Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) return null;

        try {
            return new Price(Economy.getEconomy((String) serial.get("economy")), (double) serial.get("amount"));
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
