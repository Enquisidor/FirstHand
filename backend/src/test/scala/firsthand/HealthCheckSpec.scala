package firsthand

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class HealthCheckSpec extends AnyWordSpec with Matchers {
  "HealthCheck" should {
    "always pass as a basic smoke test" in {
      // Basic smoke test to ensure test infrastructure is working
      val result = 1 + 1
      result shouldBe 2
    }
  }
}
