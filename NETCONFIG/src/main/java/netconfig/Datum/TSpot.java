package netconfig.Datum;

import java.io.Serializable;

import netconfig.Link;
import netconfig.NetconfigException;
import netconfig.Spot;
import edu.berkeley.path.bots.core.Time;

/**
 * A timestamped spot.
 * 
 * @param <LINK>
 * @author tjhunter
 * 
 *         TDO(tjhunter) add all PC fields Note: the PC -> TS functions are in
 *         arterial.coordinate
 */
public class TSpot<LINK extends Link> implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private final Spot<LINK> spot_;
    /**
     * ID of the vehicle (can be null)
     */
    private final String id_;
    /**
     * The time originating the observation. (Non null)
     */
    private final Time time_;
    /**
     * Hiring status (can be null if unavailable).
     */
    private final Boolean hired_;
    /**
     * Recorded speed at this spot. (Null if not available).
     */
    private final Float speed_;

    public TSpot(Spot<LINK> spot, String id, Time time, Boolean hired,
            Float speed) throws NetconfigException {
        if (spot == null) {
            throw new NetconfigException(new IllegalArgumentException(
                    "spot is null"), null);
        }
        if (time == null) {
            throw new NetconfigException(new IllegalArgumentException(
                    "time is null"), null);
        }
        this.spot_ = spot;
        this.id_ = id;
        this.time_ = time;
        this.hired_ = hired;
        this.speed_ = speed;
    }

    /**
     * Returns a logically equivalent probe coordinate object.
     * 
     * @return a probe coordinate representation of this object
     * @throws NetconfigException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProbeCoordinate<LINK> toProbeCoordinate() throws NetconfigException {
        Spot[] spots = { spot() };
        double[] probs = { 1.0 };
        return ProbeCoordinate.from(id(), time(), spot().toCoordinate(), spots,
                probs, null, // speed
                null, // heading
                hired(), null); // hdop
    }

    @Override
    public String toString() {
        return "TSpot[" + spot() + ", " + id() + ", " + time() + "]";
    }

    public <LINK2 extends Link> TSpot<LINK2> clone(Spot<LINK2> other)
            throws NetconfigException {
        return new TSpot<LINK2>(other, id(), time(), hired(), speed());
    }

    /**
     * @return the spot_
     */
    public Spot<LINK> spot() {
        return spot_;
    }

    /**
     * @return the id_
     */
    public String id() {
        return id_;
    }

    /**
     * @return the time_
     */
    public Time time() {
        return time_;
    }

    /**
     * @return the hired_
     */
    public Boolean hired() {
        return hired_;
    }

    /**
     * @return the speed_
     */
    public Float speed() {
        return speed_;
    }
}
