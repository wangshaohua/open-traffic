package netconfig.Datum.storage
import netconfig.Link
import netconfig.Datum.ProbeCoordinate
import core.storage.TimeRepr
import core.storage.CoordinateRepresentation
import collection.JavaConversions._
import netconfig.Datum.RouteTT
import netconfig.Datum.PathInference
import netconfig.Datum.TSpot
import bots_math.ImmutableTensor1
import com.google.common.collect.ImmutableCollection
import netconfig.Spot
import netconfig_extensions.CollectionUtils._

trait Codec[L<:Link] extends netconfig.storage.Codec[L] {

  def toRepr(pc:ProbeCoordinate[L]):ProbeCoordinateRepr = {
    val speed = if (pc.speed == null) {
      None
    } else {
      Some(pc.speed.doubleValue())
    }
    val heading = if (pc.heading == null) {
      None
    } else {
      Some(pc.heading.doubleValue())
    }
    val hired = if (pc.hired == null) {
      None
    } else {
      Some(pc.hired.booleanValue())
    }
    val hdop = if (pc.hdop == null) {
      None
    } else {
      Some(pc.hdop.floatValue())
    }
    new ProbeCoordinateRepr(
        pc.id, 
        TimeRepr.toRepr(pc.time),
        CoordinateRepresentation.toRepr(pc.coordinate),
        pc.spots.map(toRepr _),
        pc.probabilities.toSeq.map(_.doubleValue()),
        speed,
        heading,
        hired,
        hdop)
  }
  
  def toRepr(tsp:TSpot[L]):ProbeCoordinateRepr = {
    toRepr(tsp.toProbeCoordinate())
  }
  
  def toRepr(rtt:RouteTT[L]):PathInferenceRepr = {
    toRepr(rtt.toPathInference())
  }
  
  def toRepr(pi:PathInference[L]):PathInferenceRepr = {
    new PathInferenceRepr(pi.id, 
        TimeRepr.toRepr(pi.startTime),
        TimeRepr.toRepr(pi.endTime),
        pi.routes.map(toRepr _),
        pi.probabilities.toSeq.map(_.doubleValue()))
  }

  def probeCoordinateFromRepr(pcr:ProbeCoordinateRepr): ProbeCoordinate[L] = {
    val speed:java.lang.Float = pcr.speed match {
      case None => null
      case Some(x) => x.floatValue()
    }
    val heading:java.lang.Short = pcr.heading match {
      case None => null
      case Some(x) => x.shortValue()
    }
    val hired:java.lang.Boolean = pcr.hired match {
      case None => null
      case Some(x) => x.booleanValue()
    }
    val hdop:java.lang.Float = pcr.hdop match {
      case None => null
      case Some(x) => x.floatValue()
    }
    val spots = pcr.spots.map(fromRepr _).toArray
    val probs:Array[Double] = pcr.probabilities.toArray
    ProbeCoordinate.from(
        pcr.id,
        TimeRepr.fromRepr(pcr.time),
        CoordinateRepresentation.fromRepr(pcr.coordinate),
        spots,
        probs,
        speed,
        heading,
        hired,
        hdop)
  }
  
  def tSpotFromRepr(pcr:ProbeCoordinateRepr):TSpot[L] = {
    probeCoordinateFromRepr(pcr).toTSpot()
  }
  
  def pathInferenceFromRepr(pir:PathInferenceRepr):PathInference[L] = {
    val routes = pir.routes.map(fromRepr _).toArray
    val probs = pir.probabilities.toArray
    PathInference.from(pir.id, 
        TimeRepr.fromRepr(pir.startTime), 
        TimeRepr.fromRepr(pir.endTime),
        routes,
        probs, null)
     // TODO(tjh) add the hired field
  }
}









