package org.openbase.bco.bcozy.controller.powerterminal.chartattributes;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Class that describes a DateRange between two LocalDates
 */
public class DateRange {

    /**
     * Timezone that is used to obtain the current time
     */
    public static final ZoneId TIME_ZONE_ID = ZoneId.of("GMT+2");

    private LocalDate from;
    private LocalDate to;

    /**
     * Constructor
     * @param from LocalDate that marks the starting point of the DateRange
     * @param to LocalDate that marks the ending point of the DateRange
     */
    public DateRange(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    private Timestamp toTimeStamp(LocalDate localDate) {
        return Timestamp.valueOf(localDate.atTime(LocalTime.now(TIME_ZONE_ID)));
    }

    public LocalDate getStartDate() {
        return from;
    }

    public LocalDate getEndDate() {
        return to;
    }

    /**
     * Creates a Timestamp describing the starting point of the DateRange plus the current time of the day
     * @return The Timestamp
     */
    public Timestamp getStartDateAtCurrentTime() {
        return toTimeStamp(from);
    }

    /**
     * Creates a Timestamp describing the end point of the DateRange plus the current time of the day
     * @return The Timestamp
     */
    public Timestamp getEndDateAtCurrentTime() {
        return toTimeStamp(to);
    }

    /**
     * Generates a sensible interval size for charts using the DateRange
     * @return String describing the interval size in influx syntax
     */
    public String getDefaultIntervalSize() {//TODO This Belongs into the DB Handler
        int timeSpanDays = (int) DAYS.between(from, to);
        return Interval.getDefaultIntervalForTimeSpan(timeSpanDays).getInfluxIntervalString();
    }

    /**
     * Calculates if the given DateRange is valid
     * A valid DateRange is non-negative and does not reach the future
     * @param dateRange DateRange to check
     * @return Boolean describing if the DateRange is Valid
     */
    public static boolean isValid(DateRange dateRange) {
        return dateRange.getStartDate().isBefore(dateRange.getEndDate()) && dateRange.getEndDate().isBefore(LocalDate.now().plusDays(1));
    }

    /**
     * Calulates if this DateRange is valid
     * @return Boolean describing if the DateRange is Valid
     */
    public boolean isValid() {
        return isValid(this);
    }
}