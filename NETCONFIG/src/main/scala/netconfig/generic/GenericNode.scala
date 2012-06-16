package netconfig.generic

import netconfig.Node

trait GenericNode extends Node {

  val id: String
  
  override def toString: String = id

}