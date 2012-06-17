package network.gen
import netconfig.Link
import netconfig.storage.LinkIDRepr

trait GenericLink extends Link {
  val idRepr:LinkIDRepr
}