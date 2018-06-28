package tu.lambda

import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.{DiagrammedAssertions, WordSpec}

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly

class IntegrationSpec extends WordSpec with PowerMatchers  {
  "app" should {
    "work" in {
      assert(true == false)
    }
  }
}
