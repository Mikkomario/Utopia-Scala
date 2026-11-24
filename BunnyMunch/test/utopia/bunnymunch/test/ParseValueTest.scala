package utopia.bunnymunch.test

import utopia.bunnymunch.jawn.JsonBunny

import scala.io.StdIn

/**
 * @author Mikko Hilpinen
 * @since 23.11.2026, v10.0.10
 */
object ParseValueTest extends App
{
	print("Specify JSON to parse: ")
	private val input = StdIn.readLine()
	
	println(JsonBunny(input).get.valueDescription)
}
