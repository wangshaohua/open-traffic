/**
 * Copyright 2012. The Regents of the University of California (Regents).
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

package netconfig.Datum;

import netconfig.Link;
import netconfig.Route;

import com.google.common.collect.ImmutableList;

public class TrackPiece<L extends Link> {
    public static class TrackPieceConnection {
        private final int from_;
        private final int to_;

        public TrackPieceConnection(int from, int to) {
            this.from_ = from;
            this.to_ = to;
        }

        /**
         * @return the from_
         */
        public int from() {
            return from_;
        }

        /**
         * @return the to_
         */
        public int to() {
            return to_;
        }
    }

    private TrackPiece(ImmutableList<TrackPieceConnection> first_connections,
            ImmutableList<Route<L>> routes,
            ImmutableList<TrackPieceConnection> second_connections,
            ProbeCoordinate<L> point) {
        first_connections_ = first_connections;
        routes_ = routes;
        second_connections_ = second_connections;
        point_ = point;
    }

    private final ImmutableList<TrackPieceConnection> first_connections_;
    private final ImmutableList<Route<L>> routes_;
    private final ImmutableList<TrackPieceConnection> second_connections_;
    private final ProbeCoordinate<L> point_;

    /**
     * @return the first_connections_
     */
    public ImmutableList<TrackPieceConnection> firstConnections() {
        return first_connections_;
    }

    /**
     * @return the routes_
     */
    public ImmutableList<Route<L>> routes() {
        return routes_;
    }

    /**
     * @return the second_connections_
     */
    public ImmutableList<TrackPieceConnection> secondConnections() {
        return second_connections_;
    }

    /**
     * @return the point_
     */
    public ProbeCoordinate<L> point() {
        return point_;
    }

    public static <L extends Link> TrackPiece<L> from(
            ImmutableList<TrackPieceConnection> first_connections,
            ImmutableList<Route<L>> routes,
            ImmutableList<TrackPieceConnection> second_connections,
            ProbeCoordinate<L> point) {
        // TODO(?) add some sanity checks here on the input.
        return new TrackPiece<L>(first_connections, routes, second_connections,
                point);
    }
}
