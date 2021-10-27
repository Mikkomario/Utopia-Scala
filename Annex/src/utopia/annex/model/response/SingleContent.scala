package utopia.annex.model.response

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.FromModelFactory

/**
  * Used for representing contents of non-empty responses which contain model data
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
case class SingleContent[+A](model: Model)(implicit parser: FromModelFactory[A])
{
	/**
	  * Parsed response content
	  */
	lazy val parsed = parser(model)
}
