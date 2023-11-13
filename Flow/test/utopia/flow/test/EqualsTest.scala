package utopia.flow.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction

/**
  * A tests for EqualsExtensions etc.
  * @author Mikko Hilpinen
  * @since 7.8.2022, v1.16
  */
object EqualsTest extends App
{
	assert(0.23 !~== 0.55)
	assert(0.000000009 ~== 0.0)
	assert("aAa" ~== "aaa")
	assert("aba" !~== "aaa")
	assert(new TestWrapper("saa") ~== new TestWrapper("soo"))
	
	println("Success")
	
	private object TestWrapper
	{
		implicit val equals: EqualsFunction[TestWrapper] = {
			println("testing")
			_.s.head == _.s.head
		}
	}
	private class TestWrapper(val s: String)
}
