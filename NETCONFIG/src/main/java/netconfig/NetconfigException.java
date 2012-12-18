/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package netconfig;

import edu.berkeley.path.bots.core.*;

/**
 * Base class of all exceptions thrown in this package, all other exceptions
 * should be caught and re-packeged as a (sub)class of this exception.
 * <p/>
 * This class is here to make the interface to the netconfig package simpler. It
 * means that callers only _ever_ have to catch this exception, and every other
 * exception should be catght and this be rethrown.
 * <p/>
 * The other advantage is that it will by default print the stacktrace, and any
 * other messages (the NID) we want.
 * <p/>
 * Basically the message should be as complete a picture of what happened as
 * possible, so people can just call: <code><pre>
 *      Monitor.err( netconfigException );
 * </pre></code> and see everything, hence the overidden {@link #toString()},
 * {@link #getMessage()}, and {@link #getLocalizedMessage()} methods. The other
 * way, like for all throwables, will also work: <code><pre>
 *      Monitor.err( Monitor.getMessageAndStackTrace ( netconfigException ) );
 * </pre><code>
 * 
 * @see Monitor
 * @author Saneesh.Apte, samitha, Ryan Herring
 */
@SuppressWarnings("serial")
public class NetconfigException extends Exception {
    /**
     * This is what holds all the info from the instantiation arguments. It is
     * what is returned when a messgae is asked for.
     * <p/>
     * A {@link java.lang.StringBuilder} is like a
     * {@link java.lang.StringBuffer}, except it's unsynchronized.
     */
    private final StringBuilder message = new StringBuilder();

    /**
     * This holds the description of the cause of the exception.
     */
    private final String causeStr;

    /**
     * In order of causes, append the strackstrace to the message.
     * 
     * @param t
     *            the cause (a Throwable), should start with <code>this</code>.
     */
    private void appendStackTraceToMessage(Throwable t) {
        if (null == t)
            return; // End the recusion.
        // Do the previous cause (the top one gets printed first).
        this.appendStackTraceToMessage(t.getCause());
        // This is top-down (<code>this</code> class is last).
        // this.message.append( Exceptions.getStackTrace( t ) );
        this.message.append("\nCAUSE: ");
        try {
            // Cannot use getLocalizedMessage as it may not yet be created.
            if (this == t)
                this.message.append(super.getMessage());
            // Should be able to use it for the already instantiated causes.
            else
                this.message.append(t.getLocalizedMessage());
        } catch (NullPointerException npExp) { // No super
            this.message.append("Unknown");
        }
        this.message.append("\n");

    }

    /**
     * This is _the_ constrector, any argument can be null. The db, ps, and
     * cause arguments (if present) are just used to get information.
     * 
     * @param causeStr
     *            A description of the exception.
     * @param cause
     *            A root exception
     */
    public NetconfigException(Throwable cause, String causeStr) {
        // The getMessage, etc.
        // functions are ovorridden below to do something more useful in
        // most cases thas people would be catching this Exception.
        // super calls must come first (annoyingly).
        super(causeStr, cause); // null for either argument.
        // Stacktrace and cause messages for all (even this).
        this.appendStackTraceToMessage(this);
        // OK, now all the overridden methods below should work like normal.
        this.causeStr = causeStr;
    } // End big constructor.

    /**
     * Returns the constructed message
     * 
     * @return the constructed message (and stacktrace).
     */
    @Override
    public String toString() {
        return this.message.toString();
    }

    /**
     * Calls {@link #toString()}.
     * 
     * @return the same as {@link #toString()}.
     */
    @Override
    public String getMessage() {
        return this.toString();
    }

    /**
     * Returns the description of the cause of this exception
     * 
     * @return the cause of this exception
     */
    @Override
    public String getLocalizedMessage() {
        return this.causeStr;
    }

}
