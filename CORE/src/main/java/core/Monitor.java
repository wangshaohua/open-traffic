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
package core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides logging, debugging, error display, and monitoring/instrumentation.
 * 
 * <pre>
 * This class provides methods implementing:
 * 1)  Logging: The sending of normal messages to the console.
 *         Monitor.out(String) and
 *         Monitor.info(String)
 * 2)  Debugging: The sending of debug messages to the error console.
 *         Monitor.debug(String)
 * 3)  Error Display: The sending of error messages to the error console.
 *         Monitor.err(String)
 * 4)  Monitoring/Instrumentation: The sending of application status
 *     messages to the Mobile Millennium monitoring database where
 *     the messages are monitored for adherence to system metrics
 *     such as response time, etc.  The messages are also, by
 *     default, sent to the console.
 *         Monitor.mon(String note)
 *         Monitor.duration(String label, long duration)
 *         Monitor.count(String label, int count)
 *         Monitor.heartbeat(boolean isHealthy)
 * 5)   Database support to automatically know what DB to connect to.
 * 6)   Finding/recording of IP, program name, etc.
 * 
 * These methods are automatically available anywhere in your application
 * and require no setup (except ``import core.*;''). You just call them.
 * It is really as simple to use as it looks.
 * 
 * If desired you can turn the display of the message on or off for any of
 * the calls, for example:
 *     Monitor.debug = false;
 * will turn off printing of debug information.  You can also turn on or off
 * the addition of timestamp information to the message:
 *     Monitor.print_times = true;
 * 
 * A detailed example can be seen at the end of these initial comments in the
 * main() function of this class.
 * 
 * Following is a more detailed description of the class including the
 * motivations surrounding its creation and implementation.
 * 
 * 
 * This class is meant to be used by virtually every application within Mobile
 * Millennium.  It is instantiated as a singleton class by calling a private
 * constructor from the static definition.  This means it gets instantiated
 * whether you use it or not, so you should!  (I know that you can get around
 * the singleton property by using reflexivity from a privileged class, but
 * we don't need to protect against everything, just unintentional mistakes.)
 * 
 * The monitoring interface consists of 4 methods that take in a string
 * and return immediately.  This string is sent to another thread that will
 * write it to the monitoring DB.  If that second thread is blocked up, it will
 * use a little memory, but never take time away from the calling application,
 * the calling application can therefore never be hung on calls to
 *     Monitor.mon(String note)
 *     Monitor.duration(String label, long duration)
 *     Monitor.count(String label, int count)
 *     Monitor.heartbeat(boolean isHealthy)
 * 
 * and can safely send ``pings'' to the DB that will be the backend of the
 * complete system monitor.
 * 
 * The worst that can happen is that messages sent to:
 *     Monitor.mon(String note)
 *     Monitor.duration(String label, long duration)
 *     Monitor.count(String label, int count)
 *     Monitor.heartbeat(boolean isHealthy)
 * will be lost.
 * 
 * The second (and much less important) part is a transparent interface to
 * System.out.println (or .err) to provide debuglevel logging without much
 * work.  Using these functions, Monitor.{mon,out,info,err,debug}, has four
 * advantages over using <IOStream>.{err,out}.print[ln]() (System.out.println):
 * 
 *     1)   Each can be turned on and off independently and in one place so
 *          one doesn't have to comment out (or in) each call individually.
 *          For example this can be very useful for having many status prints
 *          and debug statements sprinkled through the code that can be then be
 *          turned on in one place when debugging and turned off later without
 *          affecting the monitor messages.  Also because there are four
 *          independently ones, they could be used for different parts of the
 *          application; and
 *     2)   Because all calls are routed through one place it makes it easy to
 *          do things on all messages like saving them to a file, sending them
 *          to another logger (or log4j), whatever; and
 *     3)   One can turn on printing of timestamps to every line of the output.
 *          This can make debugging much easier by basically setting up an
 *          adhoc timer by changing just one line of code in the application
 *          and then removing it later; and finally
 *     4)   Typing ``Monitor.err(String)'' is fewer keystrokes that typing
 *          ``System.err.println(String)''.  :)
 * 
 * So in short, always use these functions (mon, out, info, err, and debug)
 * instead of System.out.println() and System.err.println(), and periodically
 * send *pings* (call Monitor.mon(), duration(), or count()) so that your
 * process will show up on the system monitor.
 * 
 * Continue reading the comment in the main function (just below) for
 * usage information and examples.
 * 
 * Information on DB related uses can be found in the core.Database class.
 * </pre>
 * 
 * @author Saneesh Apte
 * 
 */
public class Monitor {
    /**
     * OK, everything below here is implementation information. Feel free to
     * keep reading, but you shouldn't have to to use this class.
     */

    /**
     * Singleton instance.
     * 
     * <pre>
     *  *  *  This is a singleton class! *  *  *
     * 
     * There are four basic ways to make a singleton class in Java:
     * 
     *  1)  Do nothing and hope that your class is instantiated only once.
     *      This is obviously a bad idea even if you could enforce it.
     *  2)  Use some variant of what is called the factory method pattern.
     *      This basically means to have a private constructor and a public
     *      static ``factory'' method that always returns the same instance,
     *      creating it the first time it is called.
     *      The major drawback of this approach is that it requires
     *      applications to keep a reference to the instance around (or get
     *      another reference) whenever they want to use it.  I wanted to
     *      make this as easy to use as the System.{out,err} classes,
     *      and felt that if I tried this not many people would use it
     *      because it would be easier to type
     *          System.out.println(String);
     *      than it would be to type
     *          monitor mon = monitor.getMonitor();
     *          mon.out(String);
     *      Or worse, some people may not realise that .getMonitor() doesn't
     *      actually create a new instance so they would carry around a
     *      reference and have to change all their function declaration
     *      (which they wouldn't do).
     *  3)  A static reference with a statically instantiated final instance
     *      and a private constructor.  This is what is done.  This means
     *      that an instance is created on JVM startup and the reference
     *      is stored in a static variable.  One can get around this
     *      with a privileged class using reflexivity, but if someone is
     *      that determined then there are much worse things they could do.
     *  4)  Use a enum with one member.  This is usually the very best way
     *      to do a singleton class.  The enum property provides an
     *      ironclad guarantee that this class will be instantiated only
     *      once, and without modifying the JVM there is no way around it.
     *      I did not use this because enums cannot implement runnable or
     *      Thread or anything else, so I would have had to have a separate
     *      private member class that implemented runnable and I think that
     *      would have made the code a little too unreadable and all
     *      communication would have to be done through functions.
     *      Overall it would just be a bit messy, and really I am not overly
     *      concerned with malicious code, so I chose (3).
     * 
     * Also see the constructor (in the INSTANCE section).
     * </pre>
     */
    private static final Monitor INSTANCE = new Monitor();

    private static BufferedReader getBufferedInputReaderInstance = null;

    /**
     * Always returns the same instance of a buffered reader to get user input,
     * it is here as a convenience b/c it is sometimes hard to know how to read
     * character streams from stdin; primarily because there are several ways to
     * do so and mostly they don't work as excepted (System.console() is
     * problematic b/c there is not a console is all cases, reading directly
     * from the inputStream (or a BufferedInputStream) does not do all the
     * character conversions properly, and having to keep yet another buffer or
     * reading one char at a time is wasteful (and slow)).
     * <p/>
     * Normally one will just want to call:
     * Monitor.getBufferedInputReader().readLine() to read a String of the next
     * line and wait until there is one.
     * 
     * @return the same instance of a BufferedReader connect to Sys...in.
     */
    public static BufferedReader getBufferedInputReader() {
        if (null == Monitor.getBufferedInputReaderInstance) {
            Monitor.getBufferedInputReaderInstance = new BufferedReader(
                    new InputStreamReader(System.in));
        }
        return Monitor.getBufferedInputReaderInstance;
    }

    /**
     * Unlike the other public functions below, setting the field of this class:
     * monitor.mon = false; does not completely disable the call, (a message
     * will always be sent to the DB) but only controls whether or not the
     * message is also printed (to .out). The analogous thing is true for both
     * monitor.duration() and monitor.count().
     * 
     * See the main function source for examples.
     */
    public static boolean mon = true; // By default print out pings.

    /**
     * This function send a generic note tho the monitoring DB (and possibly
     * print them out).
     * 
     * These functions adds a message to the queue to insert into the monitoring
     * DB. These functions will never hold up the calling process. At worst,
     * it'll throw away messages (when the queue is full).
     * 
     * @param note
     *            is a String (not an Object!) to send to the DB.
     */
    public static void mon(String note) {
        Monitor.Mesg m = Monitor.Mesg.newMesgMon(new Time(), note);
        if (Monitor.mon) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End mon()

    /** Print by default, always send to the DB. */
    public static boolean duration = true;

    /**
     * Most things are the same as the above .mon() function.
     * <p/>
     * This function takes in nanoseconds. What is returned from
     * System.nanoTime(). (Which you should always use instead of
     * System.currentTimeMillis().)
     * <p/>
     * These functions adds a message to the queue to insert into the monitoring
     * DB. These functions will never hold up the calling process. At worst,
     * it'll throw away messages (when the queue is full).
     * 
     * @param label
     *            is a String to send to the DB to say what this duration is.
     * @param duration
     *            is the duration in nanoseconds.
     */
    public static void duration(String label, long duration) {
        Monitor.Mesg m = Monitor.Mesg.newMesgDuration(new Time(), label,
                duration);
        if (Monitor.duration) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End duration()

    /** Print by default, always send to the DB. */
    public static boolean count = true; // By default print out pings.

    /**
     * Most things are the same as the above .mon() function.
     * 
     * @param label
     *            is a String to send to the DB to say what this is counting.
     * @param count
     *            is the count of label.
     */
    public static void count(String label, int count) {
        Monitor.Mesg m = Monitor.Mesg.newMesgCount(new Time(), label, count);
        if (Monitor.count) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End count()

    /** By default print out heartbeats. */
    public static boolean heartbeat = true;

    /**
     * Most things are the same as the above .mon() function.
     * 
     * @param isHealthy
     *            boolean to tell the DB if the current iteration was successful
     *            or .
     */
    public static void heartbeat(boolean isHealthy) {
        Monitor.Mesg m = Monitor.Mesg.newMesgHeartbeat(new Time(), isHealthy);
        if (Monitor.heartbeat) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End duration()

    /** Print by default, always send to the DB. */
    public static boolean alarm = true; // By default print out pings.

    /**
     * Most things are the same as the above .mon() function.
     * 
     * Use this if you want your app to send email, nowish.
     * 
     * @param note
     *            is a String to send to the DB to say what this is counting.
     */
    public static void alarm(String note) {
        Monitor.Mesg m = Monitor.Mesg.newMesgAlarm(new Time(), note);
        if (Monitor.alarm) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End alarm()

    /** Print by default, always send to the DB. */
    public static boolean percent = true; // By default print out pings.

    /**
     * Most things are the same as the above .mon() function.
     * 
     * Use this if you want your app to send email, nowish.
     * 
     * @param label
     *            is a String to send to the DB to say what this is counting.
     * @param per
     *            is percent of label (ostensibly from 0.0 to 100.0, but is
     *            unchecked, so can be negative).
     */
    public static void percent(String label, float per) {
        Monitor.Mesg m = Monitor.Mesg.newMesgPercent(new Time(), label, per);
        if (Monitor.alarm) {
            // We use System so this function can be controlled
            // independently from monitor.out().
            System.out.println(m.toString());
        }
        // We throw away messages if this is blocked up.
        Monitor.INSTANCE.fifo.offer(m);
    } // End alarm()

    public static boolean out = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     * 
     * @param o
     *            to print.
     */
    public static void out(Object o) {
        if (Monitor.out) {
            if (Monitor.print_times) {
                System.out.print((new Time()).toString() + "  ");
            }
            System.out.println(o);
        }
    }

    public static boolean info = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     */
    public static void info(Object o) {
        if (Monitor.info) {
            if (Monitor.print_times) {
                System.out.print((new Time()).toString() + "  ");
            }
            System.out.println(o);
        }
    }

    public static boolean err = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     */
    public static void err(Object o) {
        if (Monitor.err) {
            if (Monitor.print_times) {
                System.err.print((new Time()).toString() + "  ");
            }
            System.err.println(o);
        }
    }

    /** On if DB_ENV is unspecified, off otherwise. */
    public static boolean debug = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     */
    public static void debug(Object o) {
        if (Monitor.debug) {
            if (Monitor.print_times) {
                System.err.print((new Time()).toString() + "  ");
            }
            System.err.println(o);
        }
    }

    /** On if DB_ENV is unspecified, off otherwise. */
    public static boolean debug2 = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     */
    public static void debug2(Object o) {
        if (Monitor.debug2) {
            if (Monitor.print_times) {
                System.err.print((new Time()).toString() + "  ");
            }
            System.err.println(o);
        }
    }

    /** On if DB_ENV is unspecified, off otherwise. */
    public static boolean debug3 = true;

    /**
     * An interface to print.
     * 
     * If you want to turn any of these off, for instance monitor.err(), just
     * call: monitor.err = false; from somewhere.
     */
    public static void debug3(Object o) {
        if (Monitor.debug3) {
            if (Monitor.print_times) {
                System.err.print((new Time()).toString() + "  ");
            }
            System.err.println(o);
        }
    }

    /**
     * This controls if times are printed with the messages.
     * 
     * Call: monitor.print_times = true; from somewhere to turn them on.
     */
    public static boolean print_times = false;

    /**
     * These are the getters and setters for this class.
     * 
     * <pre>
     * 
     * Generally these should never be called, the values
     * should all be set correctly already, magically.
     * 
     * Please see the private constructor for more details.
     * 
     * Also these use Atomic references, so they
     * guarantee the happensbefore relationship.
     * If an application is changing these several times (or possibly at all),
     * they may take a second to be updated in either thread.  I expect that
     * this won't happen much and is not worth the overhead of a fifo.
     * They are volatile, atomic, and concurrent fields.
     * See java.util.concurrent.atomic for more information.
     * 
     * The uppercase identifers are environment variables that are used on
     * JVM startup to set these fields (that's how the mm scripts can
     * set these values from outside the Java program).
     * 
     *        ip:   MM_IP
     *              The internet protocol address of the machine the JVM
     *              is running on.
     * 
     *       pid:   MM_PID
     *              The process identifier of the JVM.
     * 
     * prog_name:   MM_PROG_NAME
     *              Identifier of what, should be ``arterial'',
     *              ``outputa'', ``pems_feed'', etc.
     * 
     *       nid:   MM_NID
     *              The Configuration identification.
     *              Should be passed in, or is otherwise 0.
     * 
     *    db_env:   MM_DB_ENV
     *              Should be passed in DB environment name (like ``dev'',
     *              ``live'', ``exp1'', etc.).  If nothing is passed in this
     *              is set to ``unspcified''.
     * 
     *  db_names:   This should not be filled on startup.
     *              This is filled by the core.Database() constructor,
     *              whenever it is called.  (MM_DB_NAMES)
     * 
     * </pre>
     */
    public static String get_ip() {
        return Monitor.INSTANCE.ip.get();
    }

    public static void set_ip(String ip) {
        Monitor.INSTANCE.ip.set(ip);
    }

    public static int get_pid() {
        return Monitor.INSTANCE.pid.get();
    }

    public static void set_pid(int pid) {
        Monitor.INSTANCE.pid.set(pid);
    }

    public static String get_prog_name() {
        return Monitor.INSTANCE.prog_name.get();
    }

    public static void set_prog_name(String prog_name) {
        Monitor.INSTANCE.prog_name.set(prog_name);
    }

    public static int get_nid() {
        return Monitor.INSTANCE.nid.get();
    }

    public static void set_nid(int nid) {
        Monitor.INSTANCE.nid.set(nid);
    }

    public static String get_db_names() {
        return Monitor.INSTANCE.db_names.get();
    }

    /**
     * 
     * The only weird one to make calls from the database constructor easier.
     */
    public static void append_db_names(String next) {
        String curr = Monitor.get_db_names();
        if (curr.length() > 0) {
            for (String str : curr.split(", ")) {
                if (str.equals(next)) {
                    return;
                }
            }
            next = curr + ", " + next;
        }
        if (!Monitor.INSTANCE.db_names.compareAndSet(curr, next)) {
            Monitor.err("Error appending db_names, this should never happen.");
        }
    }

    public static String get_db_env() {
        return Monitor.INSTANCE.db_env.get();
    }

    public static void set_db_env(String db_env) {
        // Set the thing first, then guess.
        Monitor.INSTANCE.db_env.set(db_env);
    } // end set_db_env

    /**
     * Gracefully shutdown the spawned thread. Generally this isn't called as
     * most of our programs don't exit, but if they need to (like the modified
     * streets generator) then they need to call this to not hang forever when
     * reaching the end of main().
     * 
     * Note that calling System.exit(int) will shutdown the JVM whether or not
     * this function is called. (This is only needed if we reach the end of the
     * main() function, or have joined with another thread.)
     */
    public static void shutdown() {
        if (Monitor.INSTANCE.exec != null) {
            Monitor.INSTANCE.stay_alive.set(false);
            Monitor.INSTANCE.exec.shutdownNow();
        }
    }

    /*
     * Class to send messages from producer to consumer, I mean this class is
     * the messages. And the only private static thing.
     */
    private static class Mesg {
        private enum MesgType {
            MON, DURATION, COUNT, HEARTBEAT, ALARM, PERCENT
        }

        // Universal fields.
        public final String ip;
        public final int pid;
        public final String prog_name;
        public final String db_env;
        public final int nid;
        public final String db_names;
        public final MesgType mesgType;
        public final Time time;
        // Changable, not always used fields.
        public final String note;
        public final long duration;
        public final int count;
        public final boolean isHealthy;
        public final float percent;

        private Mesg(MesgType mesgType, Time time, String note, long duration,
                int count, boolean isHealthy, float percent) {

            this.ip = Monitor.get_ip();
            this.pid = Monitor.get_pid();
            this.prog_name = Monitor.get_prog_name();
            this.db_env = Monitor.get_db_env();
            this.nid = Monitor.get_nid();
            this.db_names = Monitor.get_db_names();

            this.mesgType = mesgType;
            this.time = time;
            this.note = note;
            this.duration = duration;
            this.count = count;
            this.isHealthy = isHealthy;
            this.percent = percent;
        };

        public static Mesg newMesgMon(Time time, String note) {
            return new Mesg(MesgType.MON, time, note, 0L, 0, false, 0.0f);
        }

        public static Mesg newMesgDuration(Time time, String note, long dur) {
            return new Mesg(MesgType.DURATION, time, note, dur, 0, false, 0.0f);
        }

        public static Mesg newMesgCount(Time time, String note, int count) {
            return new Mesg(MesgType.COUNT, time, note, 0L, count, false, 0.0f);
        }

        public static Mesg newMesgHeartbeat(Time time, boolean isHealthy) {
            return new Mesg(MesgType.HEARTBEAT, time, null, 0L, 0, isHealthy,
                    0.0f);
        }

        public static Mesg newMesgAlarm(Time time, String note) {
            return new Mesg(MesgType.ALARM, time, note, 0L, 0, false, 0.0f);
        }

        public static Mesg newMesgPercent(Time time, String note, float per) {
            return new Mesg(MesgType.PERCENT, time, note, 0L, 0, false, per);
        }

        @Override
        public String toString() {
            String basicAppendBlock = String.format(
                    "  [ %s  %s %d: %s %s %d (%s) ]", this.time, this.ip,
                    this.pid, this.prog_name, this.db_env, this.nid,
                    this.db_names);
            switch (this.mesgType) {
            case MON:
                return this.note + basicAppendBlock;
            case DURATION:
                return this.note + " took "
                        + StringFormat.formatNanoPeriod(this.duration) + "."
                        + basicAppendBlock;
            case COUNT:
                return String
                        .format("Counted %d of %s.", this.count, this.note)
                        + basicAppendBlock;
            case HEARTBEAT:
                return String.format("Is healthy?  %b.", this.isHealthy)
                        + basicAppendBlock;
            case ALARM:
                return "ALARM: " + this.note + basicAppendBlock;
            case PERCENT:
                return String.format("%f % of %s.", this.percent, this.note)
                        + basicAppendBlock;
            default:
                return "Unknown mesg type" + basicAppendBlock;
            }
        }
    } // Mesg

    /**
     * Get a property, default to the env var, print a warning, and never throw
     * any exceptions.
     * 
     * @param name
     *            the key/variable/property to get the value of.
     * @return the value or null if not set or error
     */
    public static String getPropEnvValue(String name) {
        String env;
        String prop;
        // Need to trap Illegal arg exception, null pointer, and security exp.
        try {
            env = System.getenv(name);
        } catch (Exception Exp) {
            env = null;
        }
        try {
            prop = System.getProperty(name);
        } catch (Exception Exp) {
            prop = null;
        }
        // This is what should happen.
        if (null == env)
            return prop;
        // Else print a warning.
        Monitor.info(String.format("Environment variable \"%s\" used, "
                + "in the future please use properties instead.", name));
        // Else print another warning.
        Monitor.info(String.format(
                "Both a property and environment variable set:\n"
                        + "\tProperty \"%s\" = \"%s\"\n"
                        + "\tEnv var  \"%s\" = \"%s\"\n" + "Don't do this!",
                name, prop, name, env));
        if (null != prop) {
            Monitor.info("Using property.");
            return prop;
        } else {
            Monitor.info("Using environment variable (bad).");
            return env;
        }
    }

    /**
     * 
     * 
     * 
     * ^^^^^^^^^ STATIC things are above ^^^^^^^^^
     * 
     * OK everything BELOW THIS POINT is part of the instance, not the static
     * class. It should all be reachable by monitor.INSTANCE.<...>. Try not to
     * screw that up and make this class really confusing.
     * 
     * \/ \/ \/ INSTANCE things are below \/ \/ \/
     * 
     * 
     * 
     */

    /**
     * The only constructor so this class cannot be instantiated. Except, of
     * course, on startup and with a privileged class using reflexivity, but we
     * don't need to protect against _everything_.
     */
    private Monitor() {
        // Set the INSTANCE fields, but not the static ones.
        // These may not yet be set in the static context.
        Monitor.print_times = false;
        Monitor.mon = true;
        Monitor.out = true;
        Monitor.info = true;
        Monitor.err = true;
        Monitor.debug = false; // Off in colo, on if db_env is unspecified.
        Monitor.debug2 = false; // Off in colo, on if db_env is unspecified.
        Monitor.debug3 = false; // Off in colo, on if db_env is unspecified.

        /**
         * Most of the following is guesses, and the order matters.
         */

        // First we see if the IP is provided.
        this.ip.set(Monitor.getPropEnvValue("MM_IP"));
        if ((this.ip.get() == null) || (this.ip.get().length() < 7)
                || (this.ip.get().length() > 15)) {

            // Now we _guess_.
            // This is what we use if nothing else comes up.
            this.ip.set("0.0.0.0");

            // Get _all_ the IP address on the machine.
            java.util.ArrayList<short[]> ips = new java.util.ArrayList<short[]>();
            try {
                Enumeration<NetworkInterface> enumni = NetworkInterface
                        .getNetworkInterfaces();
                while (enumni.hasMoreElements()) {
                    NetworkInterface ni = enumni.nextElement();
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        byte[] ba = addrs.nextElement().getAddress();
                        ips.add(new short[] { (short) (ba[0] & 0xff),
                                (short) (ba[1] & 0xff), (short) (ba[2] & 0xff),
                                (short) (ba[3] & 0xff) });
                    }
                }

                // Now ips should contain all the IP~s.
                // Remove localhost
                ips.remove(new short[] { 127, 0, 0, 1 });
                // Guess which IP.
                // We either use the first IP that starts with ``10.'',
                // or the last one if none of 'em start with ``10.''.
                for (short[] addr : ips) {
                    this.ip.set(String.format("%d.%d.%d.%d", addr[0], addr[1],
                            addr[2], addr[3]));
                    if (addr[0] == 10) {
                        break;
                    }
                }

            } catch (SocketException se) {
                Monitor.err("Could not guess the IP.");
            }
        } // End guessing the IP.
        Monitor.info("Using ``ip'': " + this.ip.get());

        // And do the same with the PID.
        try {
            // First try the passed in value.
            this.pid.set(Integer.parseInt(Monitor.getPropEnvValue("MM_PID")));
        } catch (NumberFormatException nfe) {
            // This is _totally_ a hack to get the pid. Java should provide
            // a good, platformindependent way to get a unique ID for the
            // JVMbut they don't. There are pending ``bugs'' in Sun's
            // system to add them (Google it if you are interested), but
            // they've been open for quite awhile, so don't hold your breath.
            this.pid.set(0);
            try {
                String[] reg = (java.lang.management.ManagementFactory
                        .getRuntimeMXBean().getName()).split("@");
                if (reg.length != 2) {
                    throw new Exception("Bad regex split.");
                }
                this.pid.set(Integer.parseInt(reg[0]));
            } catch (Exception e) {
                // We don't really care what happened.
                Monitor.err(e);
                Monitor.err("Could not guess PID");
            }
        }
        Monitor.info("Using ``pid'': " + this.pid.get());

        // See if we got a name passed in.
        this.prog_name.set(Monitor.getPropEnvValue("MM_PROG_NAME"));
        if ((this.prog_name.get() == null) || (this.prog_name.get().equals(""))) {
            Monitor.err("Normal if not running in the colo: "
                    + "No prog_name passed in "
                    + "(can be set manually, if needed).");
            this.prog_name.set("unspecified");
        }
        Monitor.info("Using ``prog_name'': " + this.prog_name);

    } // End the private constructor.

    // These are all atomic. We are trading speed b/c they will usually
    // only be changed once (on startup).
    private AtomicReference<String> ip = new AtomicReference<String>();
    private AtomicInteger pid = new AtomicInteger();
    private AtomicReference<String> prog_name = new AtomicReference<String>();
    private AtomicInteger nid = new AtomicInteger();

    private AtomicReference<String> db_names = new AtomicReference<String>();

    private AtomicReference<String> db_env = new AtomicReference<String>();

    // The things not related to the external application.
    private ExecutorService exec; // Created in constructor.
    private AtomicBoolean stay_alive = new AtomicBoolean();

    // Capacity is fixed here.
    private ArrayBlockingQueue<Mesg> fifo = new ArrayBlockingQueue<Mesg>(20);

} // End class

