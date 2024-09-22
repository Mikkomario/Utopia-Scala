package utopia.scribe.core.test

import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.scribe.core.controller.logging.Synagogue

import scala.util.Try

/**
  * Tests basic error logging using the Synagogue
  * @author Mikko Hilpinen
  * @since 16.7.2023, v0.1
  */
object SynagogueTest extends App
{
	private implicit val synagogue: Synagogue = new Synagogue()
	private val failingLogger = Logger { (_, m) =>
		throw new IllegalStateException(s"Test failure (expected) - $m")
	}
	synagogue.register(failingLogger, priority = true)
	synagogue.register(SysErrLogger, priority = false)
	
	Try { throw new IllegalStateException("Original test failure (expected)") }.log
	
	println("Done")
}
