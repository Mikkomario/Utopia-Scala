package utopia.annex.model.response

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model

/**
  * Used for representing contents of non-empty responses which contain model data
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
@deprecated("Deprecated for removal. The new RequestResult structure doesn't utilize this model anymore.", "v1.8")
case class SingleContent[+A](model: Model)(implicit parser: FromModelFactory[A])
{
	/**
	  * Parsed response content
	  */
	lazy val parsed = parser(model)
}
