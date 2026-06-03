package com.laker.postman.settings;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * Typed definition for one persisted preference key.
 */
public final class SettingKey<T> {

    private final String name;
    private final T defaultValue;
    private final Function<String, T> parser;
    private final Function<T, String> formatter;
    private final UnaryOperator<T> normalizer;

    private SettingKey(String name,
                       T defaultValue,
                       Function<String, T> parser,
                       Function<T, String> formatter,
                       UnaryOperator<T> normalizer) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Setting key name must not be blank");
        }
        this.name = name;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.defaultValue = this.normalizer.apply(defaultValue);
    }

    public static <T> SettingKey<T> of(String name,
                                       T defaultValue,
                                       Function<String, T> parser,
                                       Function<T, String> formatter) {
        return new SettingKey<>(name, defaultValue, parser, formatter, UnaryOperator.identity());
    }

    public SettingKey<T> normalized(UnaryOperator<T> normalizer) {
        return new SettingKey<>(name, defaultValue, parser, formatter, normalizer);
    }

    public static SettingKey<String> stringKey(String name, String defaultValue) {
        return of(name, defaultValue, value -> value, value -> value);
    }

    public static SettingKey<Boolean> booleanKey(String name, boolean defaultValue) {
        return of(name, defaultValue, Boolean::parseBoolean, String::valueOf);
    }

    public static SettingKey<Integer> integerKey(String name, int defaultValue) {
        return of(name, defaultValue, Integer::parseInt, String::valueOf);
    }

    public static SettingKey<Integer> integerKey(String name, int defaultValue, IntUnaryOperator normalizer) {
        Objects.requireNonNull(normalizer, "normalizer");
        return integerKey(name, defaultValue).normalized(normalizer::applyAsInt);
    }

    public static SettingKey<Long> longKey(String name, long defaultValue) {
        return of(name, defaultValue, Long::parseLong, String::valueOf);
    }

    public static SettingKey<Long> longKey(String name, long defaultValue, LongUnaryOperator normalizer) {
        Objects.requireNonNull(normalizer, "normalizer");
        return longKey(name, defaultValue).normalized(normalizer::applyAsLong);
    }

    public String name() {
        return name;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public T read(Properties properties) {
        if (properties == null) {
            return defaultValue;
        }
        String rawValue = properties.getProperty(name);
        if (rawValue == null) {
            return defaultValue;
        }
        try {
            return normalizer.apply(parser.apply(rawValue));
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public void write(Properties properties, T value) {
        Objects.requireNonNull(properties, "properties");
        if (value == null) {
            properties.remove(name);
            return;
        }
        properties.setProperty(name, formatter.apply(normalizer.apply(value)));
    }
}
