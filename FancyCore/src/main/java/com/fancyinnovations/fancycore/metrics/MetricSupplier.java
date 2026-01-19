package com.fancyinnovations.fancycore.metrics;

import java.util.function.Supplier;

public record MetricSupplier<T>(String name, Supplier<T> valueSupplier) {

}