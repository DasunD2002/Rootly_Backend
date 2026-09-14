package com.backend.rootly.utility;

import java.time.Duration;
import java.time.Instant;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String formatCountdown(Instant targetDate, Instant now) {
        if (targetDate == null) {
            return "No unlock date set";
        }
        if (now.isAfter(targetDate) || now.equals(targetDate)) {
            return "Unlocked";
        }
        Duration duration = Duration.between(now, targetDate);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return days + (days == 1 ? " day " : " days ") + hours + " hrs left";
        } else if (hours > 0) {
            return hours + " hrs " + minutes + " mins left";
        } else {
            return Math.max(1, minutes) + " mins left";
        }
    }
}
