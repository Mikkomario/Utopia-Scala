package utopia.scribe.core.model.stored

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelValidationFailedException
import utopia.flow.generic.model.template.{ModelLike, Property}

/**
  * A common trait for model parsers which produce stored elements
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  */
// WET WET (Copied from Metropolis)
trait StoredFromModelFactory[+A, Data] extends FromModelFactory[A]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Factory used for parsing the data portion of the stored instance
	  */
	def dataFactory: FromModelFactory[Data]
	
	/**
	  * @param id Database id of the stored instance
	  * @param data Associated data
	  * @return A stored instance wrapping the data
	  */
	def apply(id: Int, data: Data): A
	
	
	// IMPLEMENTED	--------------------
	
	override def apply(model: ModelLike[Property]) = model("id").int
		.toTry { new ModelValidationFailedException(s"Model $model doesn't contain a valid id property") }
		.flatMap { id =>
			dataFactory(model).map { data => apply(id, data) }
		}
}
