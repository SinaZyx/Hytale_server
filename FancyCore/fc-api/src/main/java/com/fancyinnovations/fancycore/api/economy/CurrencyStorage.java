package com.fancyinnovations.fancycore.api.economy;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface CurrencyStorage {

    @ApiStatus.Internal
    List<Currency> getAllCurrencies();

    @ApiStatus.Internal
    void setCurrency(Currency currency);

    @ApiStatus.Internal
    void removeCurrency(String name);

}
