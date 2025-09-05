package com.duong.post.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class DateTimeFormatter {

    Map<Long, Function<Instant, String>> strategyMap = new LinkedHashMap<>();

    public DateTimeFormatter() {
        strategyMap.put(60L,this::formatInSeconds);
        strategyMap.put(3600L,this::formatInMinutes);
        strategyMap.put(86400L,this::formatInHours);
        strategyMap.put(Long.MAX_VALUE,this::formatInDate);
    }

    private String formatInSeconds(Instant instant){
        long elapseSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());
        return elapseSeconds + " seconds";
    }

    public String format(Instant instant) {
        long elapseSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());

         var strategy = strategyMap.entrySet()
                 .stream()
                 .filter(longFunctionEntry -> elapseSeconds < longFunctionEntry.getKey())
                 .findFirst().get();

         return strategy.getValue().apply(instant);
    }

    private String formatInMinutes(Instant instant){
        long elaspedMinutes = ChronoUnit.MINUTES.between(instant, Instant.now());
        return elaspedMinutes + " minutes";
    }

    private String formatInHours(Instant instant){
        long elapsedHours = ChronoUnit.HOURS.between(instant, Instant.now());
        return elapsedHours + " hours";
    }

    private String formatInDate(Instant instant){
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ISO_DATE;

        return localDateTime.format(dateTimeFormatter);
    }
}
