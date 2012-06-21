package netconfig.storage

/**
 * The storage representation of a spot.
 */
case class SpotRepr(
  val linkId: LinkIDRepr,
  val offset: Double) {
}