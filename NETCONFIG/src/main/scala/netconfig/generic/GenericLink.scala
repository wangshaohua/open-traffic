package netconfig.generic

import netconfig.Link
import com.google.common.collect.ImmutableCollection
import core.GeoMultiLine
import scala.annotation.unchecked.uncheckedVariance

trait GenericLink extends Link {
  val id: String
  val length: Double
  val geoMultiLine: GeoMultiLine
  def outLinks: ImmutableCollection[GenericLink]
  val inLinks: ImmutableCollection[GenericLink]

  val startNode: GenericNode

  val endNode: GenericNode

  def partialGeoMultiLine(from: Double, to: Double) = geoMultiLine.getPartialGeometry(from, to)
}