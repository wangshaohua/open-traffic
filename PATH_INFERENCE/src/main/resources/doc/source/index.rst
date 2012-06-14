
Welcome to Path Inference documentation!
=========================================

Contents:

.. toctree::
   :maxdepth: 2
   
Overview
-----------


The path inference filter (PIF in short) provides a way to match raw GPS onto a given
road network.


Quickstart - Mobille Millenniun
--------------------------------

Scala
++++++

.. highlight:: scala

Here is an example on how to use it with the Mobile Millennium code.
First load a network::
  
  import netconfig._
  import core._
  val nid = 1
  val net = new Network(Network.NetworkType.NAVTEQ,nid)


Create a source of data. We will use some data from the cabspotting feed as an example::
  
  val start_time = Time.newTimeFromBerkeleyDateTime(2010, 6, 29, 14, 0, 0, 0)
  val source = new  DataType.Cabspotting(net,start_time,1000,100,0,false)

Populate some parameters for the filter. Usually, your feed will have the same characteristics as an already existing one, so not a lot of tuning should be required.
In our example, some parameters for the feed have been alread implemented::
  
  import path_inference._
  import path_inference_mm._
  val params = new PathInferenceParameters2()
  params.fillDefaultForCabspottingOffline
  
Specify the type of output you want to get. The Mobile Millennium PIF has a number of outputs, in our case we want the most likely paths and the points::
  
  params.returnTimedSpots = true
  params.returnRouteTravelTimes = true

.. note:: You must specify an output, otherwise the filter will throw an error.

The next elements needed are models for the observations and the behaviour of the driver. You can use some reasonable models already existing as a start::
  
  val obs_model = params.getGoodObservationModelForGPSLogger
  val trans_model = params.getGoodTransitionModelForGPSLogger

Now you can create the filter::
  
  val filter=new MMPathInferenceManager(params,obs_model,trans_model,net)
  
and you are ready to use it.

Your code will have a loop that waits for some new points to be retrieved, send them to the filter, see what gets out, and write the output somewhere::
  
  while(some condition) {
    // Get some data
    val data = source.getData
    // Add the points to the filter
    filter.addPoints(points.asInstanceOf[Array[ProbeCoordinate[Link]]])
    // See if there is any output
    val timed_spots = filter.getTSpots()
    val route_travel_times = filter.getRouteTTs
    // write it somewhere if it is not empty
  }
  
When you are done, you may want to get the last point in the filter. THis is the case for example when smotthing is applied (which causes the filter to delay computations for a long time) . There is a function for that::
  
  filter.finalizeFilterBlocking
  val timed_spots = filter.getTSpots
  val route_travel_times = filter.getRouteTTs
 

That's it for now.

Java
+++++

.. highlight:: java

Same as scala, todo if someone wants to fill out.

The main difference is to use getters and setters for setting parameters::

  params.setReturnTimedSpots(true)
  params.setReturnRouteTravelTimes(true)


Python
+++++++

.. highlight:: python

Same as java.


Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

