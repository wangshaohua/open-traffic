/**
 * Copyright 2008. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package edu.berkeley.path.bots.core;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * This should be the way that MM programs refer to time in the world.
 * Conversion, or even making sure one is in the right timezone and locale over
 * disparate processes, is hard. This class aims to be easy to use and fast for
 * all things that an BOTS program might need the date and time for.
 * 
 * You shouldn't need System.currentTimeMillis(), instead just call: <code><pre>
 *      Time time = new Time();
 * </pre></code>
 * 
 * @see <a target="_top" href="http://www.odi.ch/prog/design/datetime.php">
 *      http://www.odi.ch/prog/design/datetime.php</a>
 * @author Saneesh Apte
 * @author tjhunter
 * 
 *         TODO(?) make all the internals of this class thread-safe (by using
 *         joda-time for example) TODO(?) deprecate this class?
 */
@SuppressWarnings("serial")
public class Time extends GregorianCalendar {

    /** The timezone that all BOTS things should use, for now. */
    public static final TimeZone timeZone = TimeZone
            .getTimeZone("America/Los_Angeles");
    /** Java conveniently already has one for the USA. */
    public static final Locale locale = Locale.US;
    /**
     * Static instance, time on static instantiation, but really just used to
     * pass into other functions that require an instance to pull the timezone
     * and locale. (Should not be serialized.)
     * 
     * TODO(?) deprecate
     */
    public static final transient Time staticInstantiationTime = new Time();

    /** enum to specify aggregation period in DataType functions **/
    public static enum AggregationPeriod {
        MINUTE, HOUR, DAY, WEEK, MONTH;

        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    /**
     * The primary constructor, it is the time, now-ish in Berkeley, <b>Use this
     * instead of System.currentTimeMillis()</b>. This is what is used, most of
     * the time. It's equivalent to new Time(new Date(), Time.timeZone,
     * Time.locale); which is equivalent to new Time(new Date(),
     * TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
     */
    public Time() {
        super(Time.timeZone, Time.locale);
        this.setTimeInMillis(System.currentTimeMillis());
        this.computeFields();
    }

    /**
     * Construct a Time instance for the given time, currently only
     * US/Los_Angeles and en_US are supported.
     * 
     * @param date
     *            to set the time to.
     * @param timeZone
     *            of the time in the date object.
     * @param locale
     *            of the time.
     */
    public Time(Date date, TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        this.setTime(date);
        this.computeFields(); // Not strictly necessary.
        // Right now, we only accept Los_Angeles and en_US.
        // If more are needed, then we need to change the toString
        // and ... methods.
        if (!Time.timeZone.equals(timeZone) || !Time.locale.equals(locale)) {
            throw new java.lang.IllegalArgumentException(
                    "Sorry, only US/Los_Angeles and en_US are currently supported.");
        }
    } // end constructor

    /**
     * Generator function for iPhone & emissions tracker.
     * 
     * This class is meant to take in Unix epoch time, which is in UTC and only
     * defined to the nearest second. If you need more accuracy than that or are
     * getting the value from somewhere else, use one of the other generator
     * functions, the constructor, or email the systems team to ask for help.
     * 
     * For the DB and the iPhone this is an int. That means those devices (and
     * this function) will break on Mon Jan 18 19:14:07 PST 2038.
     * 
     * @param seconds_since_epoch
     *            this is Unix time.
     * @return a newly constructed Time object.
     */
    public static Time newTimeFromEpochSeconds(int seconds_since_epoch) {
        Time ret = new Time();
        // The Date class does NOT store milliseconds, it is only precise
        // to the second. So I don't know why this takes milliseconds.
        // The Calendar class does store milliseconds
        // (and Timestamp stores nanoseconds).
        // The multiplication is done with longs b/c fn wants a long.
        ret.setTimeInMillis(1000L * seconds_since_epoch);
        ret.computeFields(); // Calc fields from the time.
        return ret;
    }

    /**
     * Create a time instance from the values given, a time and date as
     * represented in Berkeley, CA. This can end up being some very strange
     * dates if one of the input fields is out-of-bounds or otherwise
     * nonsensical.
     * 
     * @param year
     * @param month
     *            this is 1 - 12
     * @param dayOfMonth
     *            of the month
     * @param hour
     *            in a 24 hour clock
     * @param minute
     * @param second
     * @param millisecond
     * @return a newly created time object.
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public static Time newTimeFromBerkeleyDateTime(int year, int month,
            int dayOfMonth, int hour, int minute, int second, int millisecond) {

        Time ret = new Time();
        ret.clear(Time.AM_PM);
        ret.clear(Time.HOUR);
        ret.clear(Time.DAY_OF_WEEK_IN_MONTH);
        ret.clear(Time.DAY_OF_WEEK);
        ret.clear(Time.DAY_OF_YEAR);
        ret.clear(Time.WEEK_OF_YEAR);
        ret.set(Time.YEAR, year);
        ret.set(Time.MONTH, month - 1); // Month is 0 based (to be annoying).
        ret.set(Time.DAY_OF_MONTH, dayOfMonth);
        ret.set(Time.HOUR_OF_DAY, hour);
        ret.set(Time.MINUTE, minute);
        ret.set(Time.SECOND, second);
        ret.set(Time.MILLISECOND, millisecond);
        ret.complete(); // This calls computeTime first.
        return ret;
    }

    /** Just so this is not created every time. */
    private static SimpleDateFormat stringLikeBerkeleyDBSDF = null;

    /**
     * Create a Time object form a Berkeley DB string. Format: &ldquo;yyyy-MM-dd
     * HH:mm:ss.SSS&rdquo;, in 24 hour time, in the Berkeley timezone, in SQL
     * format, to millisecond accuracy.
     * 
     * @param str
     *            with the formatted string.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     * 
     *             TODO(?) accept more lenient input formats.
     */
    public static Time newTimeFromStringLikeBerkeleyDB(String str) {
        if (null == Time.stringLikeBerkeleyDBSDF) {
            Time.stringLikeBerkeleyDBSDF = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSSSSS");
            Time.stringLikeBerkeleyDBSDF.setTimeZone(Time.timeZone);
            Time.stringLikeBerkeleyDBSDF.setLenient(true);
        }
        try {
            return new Time(Time.stringLikeBerkeleyDBSDF.parse(str),
                    Time.timeZone, Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /**
     * Just calls clone. This works b/c the only two fields added so far are
     * references to a timezone and locale, which are immutable, if these became
     * mutable or we added some fields then we'd have to do something ridiculous
     * like have a private constructor or use reflection.
     * 
     * @param oldTime
     *            Time to copy.
     * @return a new Time instance with identical settings to the given one.
     */
    public static Time newTimeFromTime(Time oldTime) {
        return (Time) oldTime.clone();
    }

    /** Just so this is not created every time. */
    private static SimpleDateFormat simpleGMTSDF = null;

    /** Getter, because it is done in two places. */
    private static SimpleDateFormat getSimpleGMTSDF() {
        if (null == Time.simpleGMTSDF) {
            Time.simpleGMTSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Time.simpleGMTSDF.setTimeZone(TimeZone.getTimeZone("GMT"));
            Time.simpleGMTSDF.setLenient(false);
        }
        return Time.simpleGMTSDF;
    }

    /**
     * Create a Time object from the ICone feed.
     * 
     * @param str
     *            with the Time in GMT.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     */
    public static Time newTimeFromIConeFeed(String str) {
        try {
            return new Time(Time.getSimpleGMTSDF().parse(str), Time.timeZone,
                    Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /**
     * Create a Time object from the Info24 Taxi feed.
     * 
     * @param str
     *            with the Time in GMT.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     */
    public static Time newTimeFromInfo24TaxiFeed(String str) {
        try {
            return new Time(Time.getSimpleGMTSDF().parse(str), Time.timeZone,
                    Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /** Just so this is not created every time. */
    private static SimpleDateFormat Info24RadarSDF = null;

    /** Getter, because it is done in two places. */
    private static SimpleDateFormat getInfo24RadarSDF() {
        if (null == Time.Info24RadarSDF) {
            Time.Info24RadarSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            // = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Time.Info24RadarSDF.setTimeZone(TimeZone
                    .getTimeZone("Europe/Stockholm"));
            Time.Info24RadarSDF.setLenient(false);
        }
        return Time.Info24RadarSDF;
    }

    /**
     * Create a Time object from the Info24 Radar feed (it looks like ISO8601,
     * with that 'Z', but <b>is not Zulu time</b>, it is in Stockholm local time
     * (but Daylight Savings don't happen at exactly the right time, so this'll
     * be wrong during that period)). This assumes that the &ldquo;T&rdquo; and
     * the &ldquo;Z&rdquo; are always present in the string, and that there are
     * 3 digits for milliseconds, will throw an {@link IllegalArgumentException}
     * if not.
     * 
     * @param str
     *            with the Time in what looks like ISO8601.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     */
    public static Time newTimeFromInfo24RadarFeed(String str) {
        try {
            return new Time(Time.getInfo24RadarSDF().parse(str), Time.timeZone,
                    Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /** Just so this is not created every time. */
    private static SimpleDateFormat PeMSSDF = null;

    /** Getter, because it is done in two places. */
    private static SimpleDateFormat getPeMSSDF() {
        if (null == Time.PeMSSDF) {
            Time.PeMSSDF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Time.PeMSSDF.setTimeZone(Time.timeZone);
            Time.PeMSSDF.setLenient(false);
        }
        return Time.PeMSSDF;
    }

    /**
     * Create a Time object from the PeMS feed, which is in Berkeley time. The
     * input string is &ldquo;MM/dd/yyyy HH:mm:ss&rdquo;, in 24 hour time, it
     * will throw an {@link IllegalArgumentException} if it is not.
     * 
     * @param str
     *            with the Time.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     */
    public static Time newTimeFromPeMSFeed(String str) {
        try {
            return new Time(Time.getPeMSSDF().parse(str), Time.timeZone,
                    Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /** Just so this is not created every time. */
    private static SimpleDateFormat combinedISO8601SDF = null;

    /** Getter, because it is done in two places. */
    private static SimpleDateFormat getCombinedISO8601SDF() {
        if (null == Time.combinedISO8601SDF) {
            Time.combinedISO8601SDF = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'");
            Time.combinedISO8601SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
            Time.combinedISO8601SDF.setLenient(false);
        }
        return Time.combinedISO8601SDF;
    }

    /**
     * Create a Time object from the combined date and time variant of ISO8601,
     * with that 'Z' at the end. This assumes that the &ldquo;T&rdquo; and the
     * &ldquo;Z&rdquo; are always present in the string, and that there are no
     * milliseconds.
     * 
     * @param str
     *            with the Time in combined ISO8601.
     * @return a newly constructed Time Object.
     * @throws IllegalArgumentException
     *             on any parse error.
     */
    public static Time newTimeFromCombinedISO8601(String str) {
        try {
            return new Time(Time.getCombinedISO8601SDF().parse(str),
                    Time.timeZone, Time.locale);
        } catch (ParseException pExp) {
            throw new IllegalArgumentException(
                    "The argument string could not parsed.", pExp);
        }
    }

    /**
     * 
     * 
     * Instance fields/functions below.
     * 
     * 
     */
    /**
     * Changes the time represented by this instance by the given number of
     * (fractional) seconds.
     * 
     * @param interval
     *            to add to this, in (fractional) seconds.
     * @throws IllegalArgumentException
     *             if interval is NaN or infinite.
     */
    public void add(float interval) {
        if (Float.isNaN(interval) || Float.isInfinite(interval)) {
            throw new IllegalArgumentException(
                    "Given interval is NaN or infinite.");
        }
        if (interval > Integer.MAX_VALUE || interval < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Given interval out of range.");
        }
        int ipart = (int) interval;
        int fpart = Math.round(1000f * (interval - (float) ipart));
        this.add(Time.SECOND, ipart);
        this.add(Time.MILLISECOND, fpart);
    }

    /**
     * String representation of this Time instance, in
     * "YYYY-MM-DD HH:MM:SS.MMMMMM". This string representation is human
     * readable, sortable, constant length, and cast-able to a PostgreSQL
     * timestamp type (really important that it be cast-able).
     * <p/>
     * This representation cannot be changed without also changing the
     * {@link Database#psSetTimestamp(java.lang.String, int, core.Time)}
     * function, because it uses set object.
     * 
     * @return the Time as a string.
     */
    @Override
    public String toString() {
        return String.format(
                "%04d-%02d-%02d %02d:%02d:%02d.%03d",
                this.get(Time.YEAR),
                // Annoyingly this is [0, 12] (lunar cals have 13 months).
                1 + this.get(Time.MONTH), this.get(Time.DAY_OF_MONTH),
                this.get(Time.HOUR_OF_DAY), this.get(Time.MINUTE),
                this.get(Time.SECOND), this.get(Time.MILLISECOND));
    }

    public String toStringWithoutSeconds() {
        return String.format("%04d-%02d-%02d %02d:%02d", this.get(Time.YEAR),
                // Annoyingly this is [0, 12] (lunar cals have 13 months).
                1 + this.get(Time.MONTH), this.get(Time.DAY_OF_MONTH),
                this.get(Time.HOUR_OF_DAY), this.get(Time.MINUTE));
    }

    /**
     * Helper function, asked for once or twice. Just calls
     * {@link GregorianCalendar#getDisplayName(int, int, java.util.Locale)}
     * 
     * @return String with the full name of the month.
     * @see GregorianCalendar#getDisplayName(int, int, java.util.Locale)
     */
    public String getDisplayNameMonthLong() {
        return this.getDisplayName(Time.MONTH, Time.LONG, Time.locale);
    }

    /**
     * Helper function, asked for once or twice. Just calls
     * {@link GregorianCalendar#getDisplayName(int, int, java.util.Locale)}
     * 
     * @return String with the abbreviated name of the month.
     * @see GregorianCalendar#getDisplayName(int, int, java.util.Locale)
     */
    public String getDisplayNameMonthShort() {
        return this.getDisplayName(Time.MONTH, Time.SHORT, Time.locale);
    }

    /**
     * Helper function, asked for once or twice. Just calls
     * {@link GregorianCalendar#getDisplayName(int, int, java.util.Locale)}
     * 
     * @return String with the full name of the day of the week.
     * @see GregorianCalendar#getDisplayName(int, int, java.util.Locale)
     */
    public String getDisplayNameDayLong() {
        return this.getDisplayName(Time.DAY_OF_WEEK, Time.LONG, Time.locale);
    }

    /**
     * Helper function, asked for once or twice. Just calls
     * {@link GregorianCalendar#getDisplayName(int, int, java.util.Locale)}
     * 
     * @return String with the abbreviated name of the day of the week.
     * @see GregorianCalendar#getDisplayName(int, int, java.util.Locale)
     */
    public String getDisplayNameDayShort() {
        return this.getDisplayName(Time.DAY_OF_WEEK, Time.SHORT, Time.locale);
    }

    /**
     * Return numeric four-digit year.
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getYear() {
        return this.get(Time.YEAR);
    }

    /**
     * Return numeric month ( 1-based, that is, returns 1 to 12 ).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getMonth() {
        return this.get(Time.MONTH) + 1; // Month is 0 based (to be annoying).
    }

    /**
     * Return numeric day of month (1 to 31).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getDayOfMonth() {
        return this.get(Time.DAY_OF_MONTH);
    }

    /**
     * Returns numeric value day of week (1 to 7). Sunday is 1, Saturday is 7.
     * 
     * @return An integer value for the day of week (Sunday = 1 through Saturday
     *         = 7)
     */
    public int getDayOfWeek() {
        return this.get(Time.DAY_OF_WEEK);
    }

    /**
     * Return numeric hour (0 to 23).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getHour() {
        return this.get(Time.HOUR_OF_DAY);
    }

    /**
     * Return numeric minute (0 to 59).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getMinute() {
        return this.get(Time.MINUTE);
    }

    /**
     * Return numeric second (0 to 60 (with leap seconds)).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getSecond() {
        return this.get(Time.SECOND);
    }

    /**
     * Return numeric second (0 to 60 (with leap seconds)).
     * <p/>
     * These functions are here so that the Berkeley Time constructor defaults
     * match these (only month is really different).
     * 
     * @see #getYear
     * @see #getMonth
     * @see #getDayOfMonth
     * @see #getHour
     * @see #getMinute
     * @see #getSecond
     * @see #getMillisecond
     * @see java.util.Calendar#get(int)
     */
    public int getMillisecond() {
        return this.get(Time.MILLISECOND);
    }

    /**
     * Return this time object as a I Cone feed-style string.
     * 
     * @return this time object as a I Cone feed-style string.
     */
    public String toIconeFeedString() {
        return Time.getSimpleGMTSDF().format(this.getTime());
    }

    /**
     * Return the difference (in seconds) with an other time (argument) taking
     * into account leap days & seconds, daylight savings time, and other random
     * adjustments.
     * 
     * @param anotherTime
     *            can be smaller or greater.
     * @return A float representing the number of seconds between times.
     */
    public float secondsSince(Time anotherTime) {
        // Both times are guaranteed to be the same time zone and locale.
        long diff = this.getTimeInMillis() - anotherTime.getTimeInMillis();
        // Casting to a float directly may loose precision (within a milli)
        // if the difference is big enough, so we use a double first and
        // then cast it.
        return (float) (((double) diff) / 1000d);
    }

    /** Convenience function for Scala users. */
    public boolean $greater(Time that) {
        return this.compareTo(that) > 0;
    }

    /** Convenience function for Scala users. */
    public boolean $greater$eq(Time that) {
        return this.compareTo(that) >= 0;
    }

    /** Convenience function for Scala users. */
    public boolean $less$eq(Time that) {
        return this.compareTo(that) <= 0;
    }

    /** Convenience function for Scala users. */
    public boolean $less(Time that) {
        return this.compareTo(that) < 0;
    }

    /** Convenience function for Scala users. */
    public boolean $eq$eq(Time that) {
        if (that == null)
            return false;
        return this.compareTo(that) == 0;
    }

    /**
     * Convenience function for Scala users, difference between two times, in
     * seconds.
     * 
     * @param that
     *            other Time instance to compare to.
     * @return The difference between two times, in seconds.
     * @deprecated Users should use the Duration class in Joda-time.
     */
    @Deprecated
    public float $minus(Time that) {
        return this.secondsSince(that);
    }

    /**
     * Creates a new time plus a sertain offset
     * 
     * @param interval
     *            of time, in seconds.
     * @return The time plus the interval added.
     * @deprecated Users should use the Duration class in Joda-time.
     */
    @Deprecated
    public Time plus(float interval) {
        Time t = (Time) this.clone();
        t.add(interval);
        return t;
    }

    /**
     * Convenience function for scala users.
     * 
     * @param interval
     *            of time, in seconds.
     * @return The time plus the interval added.
     * @deprecated Users should use the Duration class in Joda-time.
     */
    @Deprecated
    public Time $plus(float interval) {
        Time t = (Time) this.clone();
        t.add(interval);
        return t;
    }

    /**
     * @return the equivalent time representation in Joda-time class.
     */
    public DateTime toDateTime() {
        return new DateTime(getYear(), getMonth(), getDayOfMonth(), getHour(),
                getMinute(), getSecond(), getMillisecond(),
                DateTimeZone.forID("America/Los_Angeles"));
    }

    /**
     * Returns the equivalent representation of a DateTime object in Time class.
     * 
     * @param dt
     * @return
     */
    public static Time from(DateTime dt) {
        Date d = dt.toDate();
        return new Time(d, timeZone, locale);
    }

} // end class
