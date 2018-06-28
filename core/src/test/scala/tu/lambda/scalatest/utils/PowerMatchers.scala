package tu.lambda.scalatest.utils

import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.DiagrammedAssertions

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly
