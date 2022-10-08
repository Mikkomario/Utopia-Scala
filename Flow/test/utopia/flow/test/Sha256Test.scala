package utopia.flow.test

import utopia.flow.parse.Sha256Hasher

import scala.io.StdIn

/**
  * A simple sha 256 hashing test
  * @author Mikko Hilpinen
  * @since 1.3.2022, v1.15
  */
object Sha256Test extends App
{
	println("Input a string to hash")
	val input = StdIn.readLine()
	
	println(Sha256Hasher(input))
}
