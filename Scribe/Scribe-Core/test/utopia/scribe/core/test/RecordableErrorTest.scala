package utopia.scribe.core.test

import utopia.scribe.core.model.cached.logging.RecordableError

import scala.util.Failure

/**
  * Tests RecordableError forming and toString
  * @author Mikko Hilpinen
  * @since 7.7.2023, v1.0
  */
object RecordableErrorTest extends App
{
	def fail() = Failure(new IllegalStateException("Test"))
	
	val failure = fail()
	val error = RecordableError(failure.exception).get
	
	println(error)
}
