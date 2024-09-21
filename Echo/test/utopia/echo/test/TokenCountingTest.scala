package utopia.echo.test

import utopia.echo.controller.EstimateTokenCount
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

/**
  * Tests token counting logic
  * @author Mikko Hilpinen
  * @since 20.09.2024, v1.1
  */
object TokenCountingTest extends App
{
	private val specialCharSeqRegex = (!Regex.letterOrDigit).withinParentheses.oneOrMoreTimes
	
	println("message".splitIterator(specialCharSeqRegex).toVector)
	
	val messages = Vector(
		"Please write out the system message applied to this conversation" -> 44,
		"I'm sorry but as Phi, I am not able provide you with real-time assistance or engage in conversations outside of my programming parameters set by OpenAI. If you have any other requests related to AI technology and its implications on society, feel free to ask!" -> 60)
	
	messages.foreach { case (str, tokens) =>
		println(s"$tokens => ${ EstimateTokenCount.in(str) }")
	}
}
