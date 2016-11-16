import org.scalatest._

class HelloSpec2 extends FlatSpec with Matchers {
  "Hello" should "have tests" in {
    true should ===(true)
  }
}

