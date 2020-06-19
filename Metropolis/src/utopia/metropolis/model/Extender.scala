package utopia.metropolis.model

import scala.language.implicitConversions

object Extender
{
	implicit def autoAccess[A](e: Extender[A]): A = e.wrapped
	
	// implicit def deepAutoAccess[Deep, Surface](e: Extender[Surface])(implicit f: Surface => Deep): Deep = f(e.wrapped)
}

/**
  * A common traits for models that wrap other models, providing additional functionality
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
trait Extender[+A]
{
	/**
	  * @return Wrapped item
	  */
	def wrapped: A
}
