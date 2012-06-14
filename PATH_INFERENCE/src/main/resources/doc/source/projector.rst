Point projection operations
============================

Note on testing code
---------------------

You can launch a scala console with all the jar file loaded properly using the command::

  $MM_HOME/PATH_INFERENCE/scripts/debug.sh

Everything down there works in this environment.

The *Path Inference* project includes some structures to perform projections onto a road network (but it could be any structures defined by 2-dimensional segments).

Mobile Millennium, scala
-------------------------

Here is how to use it in scala, using the **Mobile Millennium** *Network* class.

First load a network::

  import netconfig._
  val net = new Network(Network.NetworkType.NAVTEQ,1)
  
Load the structures for the projection::

  import path_inference.projection._
  import test_network.Coordinate.CoreCoordinateOperations
  val projector = new KDProjector(net.getNavteqLinks)(CoreCoordinateOperations)

And now the function you want to use is *projector.getClosestLinks*::

  val all_links = net.getNavteqLinks
  val max_radius = 50.0
  val max_num_projections = 40

  val c = all_links.head.getGeoMultiLine.getFirstCoordinate
  val projections = projector.getClosestLinks(c,max_radius,max_num_projections)
  
Testing performance
--------------------

And now a small performance loop::

  import core._
  val all_links = net.getNavteqLinks
  val max_radius = 50.0
  val max_num_projections = 40
  
  val t1 = new Time()
  var x = 0
  for(i <- 0 until 10)
    for(l <- all_links) {
      val c = l.getGeoMultiLine.getFirstCoordinate  
      val projections = projector.getClosestLinks(c,max_radius,max_num_projections)
      // Loop through every projection
      for(p <- projections) {
        val link = p._1
        val offset = p._2
      }
      x += projections.length
    }
  val t2 = new Time()
  10 *  all_links.length / ((t2 - t1))
  