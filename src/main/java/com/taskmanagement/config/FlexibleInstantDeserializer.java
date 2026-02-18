package com.taskmanagement.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializes ISO-8601 date-time strings into {@link Instant}, accepting
 * formats that omit seconds or timezone (e.g. "2026-02-18T14:08").
 * Values without timezone are interpreted in the system default zone.
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    /** Accepts "2026-02-18T14:08" (no seconds). */
    private static final DateTimeFormatter DATE_TIME_NO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();

        // Full ISO-8601 with zone/offset (e.g. 2026-02-18T14:08:00Z, 2026-02-18T14:08:00+05:30)
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through to try other formats
        }

        ZoneId zone = ZoneId.systemDefault();

        // Date-time without seconds: 2026-02-18T14:08
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, DATE_TIME_NO_SECONDS);
            return ldt.atZone(zone).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // Date-time with seconds but no zone: 2026-02-18T14:08:00
        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return ldt.atZone(zone).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // Date only: 2026-02-18 (start of day in system zone)
        try {
            LocalDate date = LocalDate.parse(value, DATE_ONLY);
            return date.atStartOfDay(zone).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        throw new IllegalArgumentException(
                "Cannot parse date-time '%s'. Use ISO-8601 format, e.g. 2026-02-18T14:08, 2026-02-18T14:08:00, or 2026-02-18T14:08:00Z".formatted(value)
        );
    }
}
