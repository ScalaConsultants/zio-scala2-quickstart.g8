package $package$.interop.akka

import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import zio.test.DefaultRunnableSpec

trait ZioRouteTest extends DefaultRunnableSpec with TestFrameworkInterface with RouteTest {

  def failTest(msg: String): Nothing = throw new Exception(msg)
}
