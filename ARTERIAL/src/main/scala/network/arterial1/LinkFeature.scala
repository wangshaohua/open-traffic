package network.arterial1

/**
 * Encodes a specific feature at the end of the link.
 */
sealed trait LinkFeature {
  def string: String
}

object Stop extends LinkFeature {
  def string = "stop"
}

object Light extends LinkFeature {
  def string = "light"
}

object PedestrianCross extends LinkFeature {
  def string = "cross"
}

object NoFeature extends LinkFeature {
  def string = "nothing"
}

object LinkFeature {
  def decode(s:String):Option[LinkFeature] = s match {
    case "stop" => Some(Stop)
    case "light" => Some(Light)
    case "cross" => Some(PedestrianCross)
    case "nothing" => Some(NoFeature)
    case _ => None
  }
}