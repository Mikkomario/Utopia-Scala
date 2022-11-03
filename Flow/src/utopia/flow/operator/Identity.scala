package utopia.flow.operator

import scala.language.implicitConversions

/**
  * An identity function. Returns the item as it is.
  * @author Mikko Hilpinen
  * @since 30.9.2022, v2.0
  */
object Identity
{
	def apply[A](item: A): A = item
	
	implicit def identityFunction[A](i: Identity.type): Function[A, A] = i.apply[A]
}
