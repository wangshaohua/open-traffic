package netconfig.test
import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import _root_.org.junit.Test
import org.scalacheck.Arbitrary
import netconfig.Route
import netconfig.Link
import org.scalatest.junit.JUnitSuite
import network.gen.test.SimpleGen

trait RouteTests extends Checkers {

  implicit def routes: Arbitrary[Route[Link]]

  @Test
  def testBasic() {
    check((r: Route[Link]) => {
      r.length() >= 0
    })
    check((r: Route[Link]) => {
      r.spots.size() >= 2
    })
    check((r: Route[Link]) => {
      r.links.size() >= 1
    })
  }
}

class Route1Test extends JUnitSuite with RouteTests {
  val numLinks = 5
  lazy val net = SimpleGen.line(5)
  def routes = Arbitrary(RouteGen.genSmallRoutes(net, numLinks))

  // Somehow, this dummy test seems necessary in eclipse??
  @Test
  def testBasic0() {}
}