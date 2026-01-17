package com.fancyinnovations.fancycore.economy.storage.fake;

import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyStorage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyFakeStorage implements CurrencyStorage {

    private final Map<String, Currency> currencies;

    public CurrencyFakeStorage() {
        this.currencies = new ConcurrentHashMap<>();
    }

    @Override
    public List<Currency> getAllCurrencies() {
        return List.copyOf(currencies.values());
    }

    @Override
    public void setCurrency(Currency currency) {
        currencies.put(currency.name(), currency);
    }

    @Override
    public void removeCurrency(String name) {
        currencies.remove(name);
    }
}
