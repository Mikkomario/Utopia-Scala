package utopia.metropolis.model.stored

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

/**
  * A common trait for model parsers which produce stored elements
  * @author Mikko Hilpinen
  * @since 19.6.2020, v1
  */
@deprecated("Please use the Vault version instead", "v2.3")
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
	
	override def apply(model: HasProperties) = model("id").int
		.toTry { new IllegalArgumentException(s"Model $model doesn't contain a valid id property") }
		.flatMap { id =>
			dataFactory(model).map { data => apply(id, data) }
		}
}
