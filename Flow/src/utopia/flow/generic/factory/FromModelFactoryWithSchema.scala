package utopia.flow.generic.factory

import utopia.flow.generic.model.template
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.template.Property

/**
  * This factory uses a schema to validate the model before attempting parse
  * @author Mikko Hilpinen
  * @since 30.7.2019, v1.6+
  */
trait FromModelFactoryWithSchema[+A] extends FromModelFactory[A]
{
	// ABSTRACT	-----------------------
	
	/**
	  * Schema used when validating models
	  */
	def schema: ModelDeclaration
	
	/**
	  * Parses an already validated model. This method is expected to always succeed.
	  * @param model Model that has already been validated using 'schema'
	  * @return Parsed item
	  */
	protected def fromValidatedModel(model: Model): A
	
	
	// IMPLEMENTED	------------------
	
	override def apply(model: template.ModelLike[Property]) = schema.validate(model).map(fromValidatedModel)
}
