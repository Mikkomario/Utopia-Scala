package utopia.scribe.core.test

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.test.TestContext._
import utopia.scribe.core.controller.logging.ConsoleScribe
import utopia.scribe.core.model.enumeration.Severity.Debug

/**
  * Tests the ConsoleScribe's interaction with the console
  * @author Mikko Hilpinen
  * @since 03.10.2024, v1.1
  */
object ConsoleScribeTest extends App
{
	val log = ConsoleScribe("test", defaultSeverity = Debug)
	
	println("Println before logging")
	log("Testing logging", Model.from("detail" -> "value"))
	println("Println after logging")
}
