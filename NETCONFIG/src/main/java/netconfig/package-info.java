
/**
 * <b>The primary package to be used by all people writing models requiring a road network.</b>
 * The most critical classes in this package are {@link netconfig.Datum}.
 *
 * <p>
 * All data sources have some sort of data structure and all data that comes into
 * the system is associated with a time.  <b>These structures are all
 * located in the {@link netconfig.Datum} subclasses.</b>  For example, 
 * {@link netconfig.Datum.ProbeCoordinate}
 * is used to store the information needed to describe a single raw GPS measurement
 * from any probe data provider.
 * </p>
 *
 * <p>
 * Any needed data structures beyond the ones that directly relate to measurement data
 * are contained in their own classes within this package.  Examples of non-measurement
 * data structures are {@link netconfig.Link} (a link of the road network),
 * {@link netconfig.Node} (a node in the road network),
 * {@link netconfig.Spot} (a location on the road network), and
 * {@link netconfig.Route} (a structure that defines a route through a network), among others.
 * </p>
 * @author Ryan Herring, san, Olli-Pekka
 */
package netconfig;
