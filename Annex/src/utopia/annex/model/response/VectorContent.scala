package utopia.annex.model.response

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.FromModelFactory
import utopia.flow.util.CollectionExtensions._

/**
  * Used for representing content read from a non-empty response in vector/array format
  * @author Mikko
  * @since 14.6.2020, v1
  */
case class VectorContent[+A](models: Vector[Model[Constant]])(implicit parser: FromModelFactory[A])
{
	/**
	  * Parsed response content
	  */
	lazy val parsed = models.tryMap { parser(_) }
}
