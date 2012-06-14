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

/**
 * There are many convienience functions missing from the Java std lib and I
 * find myself re-implementing some of them frequently, so I finally just made a
 * place for these functions. I'm not going to copy over all that's already been
 * implemented, but will ass to this as time goes on.
 */
public class StringFormat {
    /**
     * Mostly a helper method. Whatever type Object really is the elements
     * (unless arr is null, which is allowed) must either be themselves null or
     * must implement the toString() method. Noteably this function will fail
     * for primitive array types (use the functions in java.util.Arrays
     * instead).
     * 
     * @param arr
     *            to convert
     * @return "null" if arr is null, { v1, v2, v3, ... } with vX = "null" if
     *         the element itself is null, or the same as: { arr[0].toString(),
     *         arr[1].toString(), ... } if not.
     */
    public static String ObjectArray(Object[] arr) {
        if (null == arr)
            return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for (Object o : arr) {
            if (null == o)
                sb.append("null");
            else
                sb.append(o.toString());
            sb.append(", ");
        }
        // Remove the last two chars.
        sb.setLength(sb.length() - 2);
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Convert the given time-span (in nanoseconds) to a human-readable string
     * representation.
     * 
     * @param nanos
     *            the duration to convert.
     * @return A string in minutes and seconds representing the given time-span,
     *         the string is: ``%d mins, %f secs''.
     */
    public static String formatNanoPeriod(long nanos) {
        // Convert to mins + seconds.
        return String.format("%d mins, %f secs", (nanos / 60000000000L),
                (float) (nanos % 60000000000L) / 1000000000f);
    }

    /**
     * Format the given duration to a human-readable string. In the core classes
     * duration should always be in (fractional) seconds, stored in a Float.
     * 
     * @param duration
     *            in (fractional) seconds.
     * @return A human-readable string representation.
     */
    public static String formatDuration(Float duration) {
        try {
            return StringFormat
                    .formatNanoPeriod((long) (duration * 1000000000));
        } catch (NullPointerException npExp) {
            return "(null)";
        }
    }
}
