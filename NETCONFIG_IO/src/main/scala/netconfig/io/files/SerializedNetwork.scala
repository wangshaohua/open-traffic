package netconfig.io.files

object SerializedNetwork {
  def fileName(nid: Int, net_type: String): String = {
    "%s/network_nid%d_%s.json.gz".format(Files.dataDir(), nid, net_type)

  }

}