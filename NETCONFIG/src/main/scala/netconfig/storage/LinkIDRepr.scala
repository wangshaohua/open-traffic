package netconfig.storage

/**
 * A generic representation of the identifier of a link.
 *
 * It is assumed that any link of any road network can be uniquely identified
 * by a pair of a long integer and of an integer.
 */
case class LinkIDRepr(
  var primary: Long,
  var secondary: Int) {
}