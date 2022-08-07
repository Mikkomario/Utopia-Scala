package utopia.flow.util

/**
  * Introduces some extensions for comparable items
  * @author Mikko Hilpinen
  * @since 7.8.2022, v1.16
  */
object EqualsExtensions
{
	implicit def doubleEquals: EqualsFunction[Double] = EqualsFunction.approxDouble
	implicit def stringEquals: EqualsFunction[String] = EqualsFunction.stringCaseInsensitive
	
	implicit class ApproxEquals[+A](val a: A) extends AnyVal
	{
		/**
		  * Tests whether these two items compare with each other using a custom equals function
		  * @param other Another item
		  * @param equals An implicit equality function to use
		  * @tparam B Type of the compared item
		  * @return Whether these two items may be considered equal under the specified function
		  */
		def ~==[B >: A](other: B)(implicit equals: EqualsFunction[B]): Boolean = equals(a, other)
		/**
		  * Tests whether these two items don't compare with each other using a custom equals function
		  * @param other Another item
		  * @param equals An implicit equality function to use
		  * @tparam B Type of the compared item
		  * @return Whether these two items may not be considered equal under the specified function
		  */
		def !~==[B >: A](other: B)(implicit equals: EqualsFunction[B]): Boolean = equals.not(a, other)
	}
}
