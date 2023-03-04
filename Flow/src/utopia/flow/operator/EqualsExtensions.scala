package utopia.flow.operator

/**
  * Introduces some extensions for comparable items
  * @author Mikko Hilpinen
  * @since 7.8.2022, v1.16
  */
object EqualsExtensions
{
	implicit def doubleEquals: EqualsFunction[Double] = EqualsFunction.approxDouble
	implicit def stringEquals: EqualsFunction[String] = EqualsFunction.stringCaseInsensitive
	
	implicit class ImplicitApproxEquals[+A](val a: A) extends AnyVal
	{
		/**
		  * Tests whether these two items compare with each other using a custom equals function
		  * @param other  Another item
		  * @param equals An implicit equality function to use
		  * @tparam B Type of the compared item
		  * @return Whether these two items may be considered equal under the specified function
		  */
		def ~==[B >: A](other: B)(implicit equals: EqualsFunction[B]): Boolean = equals(a, other)
		
		/**
		  * Tests whether these two items don't compare with each other using a custom equals function
		  * @param other  Another item
		  * @param equals An implicit equality function to use
		  * @tparam B Type of the compared item
		  * @return Whether these two items may not be considered equal under the specified function
		  */
		def !~==[B >: A](other: B)(implicit equals: EqualsFunction[B]): Boolean = equals.not(a, other)
	}
	
	implicit class ImplicitApproxEqualsCollection[+A](val c: Iterable[A]) extends AnyVal
	{
		/**
		  * Checks whether another collection has identical contents, when using the specified equals function
		  * @param other Another collection
		  * @param equals An equals function
		  * @tparam B Type of the items in the other collection
		  * @return Whether these collections have identical items, when using the specified equals function
		  */
		def hasEqualContentWith[B >: A](other: Iterable[B])(implicit equals: EqualsFunction[B]) =
			c.sizeCompare(other) == 0 && c.forall { a => other.exists { _ ~== a } }
	}
	
	implicit class ApproxEqualsOption[+A](val o: Option[A]) extends AnyVal
	{
		def ~==[B >: A](other: Option[B])(implicit eq: EqualsFunction[B]) = o match {
			case Some(a) => other.exists { b => a ~== b }
			case None => other.isEmpty
		}
	}
}
