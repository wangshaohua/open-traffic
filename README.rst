BOTS: the Berkeley Open Traffic Stack
=======================================

This repository contains the Mobile Millennium code publicly released under an open-source license. It includes so 
far the following packages:

 1. CORE Some core structures to represent time and geometries of roads
 2. NETCONFIG Structures to represent map-matched data on road networks.
 3. PATH_INFERENCE The path inference filter.

Here are a few instructions for downloading and compiling the code on linux with limited
configuration.

Getting started - linux
------------------------

Download and install maven version 3.x::
  
  http://maven.apache.org/download.html

Make sure the maven executable (mvn) is in the path.
Download the source code::


  git clone git@github.com:calpath/open-traffic.git

Compile and install the project::

  cd open-traffic
  mvn clean compile install

All the jar files will be installed in ~/.m2/repository/edu/berkeley/traffic.
If you want to edit some code, delete this directory and run mvn compile install again.


Documentation
---------------

Some documentation is being put together for using the path inference filter. Meanwhile, the code documentation
can be extracted using javadoc and scaladoc::

  mvn javadoc:javadoc
  mvn scala:doc 

License
--------

All the code is released under the Apache 2.0 license.