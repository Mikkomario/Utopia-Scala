package utopia.annex.model.response

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.collection.CollectionExtensions._

/**
  * Used for representing content read from a non-empty response in vector/array format
  * @author Mikko
  * @since 14.6.2020, v1
  */
case class VectorContent[+A](models: Vector[Model])(implicit parser: FromModelFactory[A])
{
	/**
	  * Parsed response content
	  */
	lazy val parsed = models.tryMap { parser(_) }
}
