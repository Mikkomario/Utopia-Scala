package utopia.echo.model

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.Try

object Reply extends FromModelFactory[Reply]
{
	// ATTRIBUTES   -------------------------
	
	// lazy val streamedResponseParser = new StreamedModelsResponseParser(this, ???)
	
	
	// IMPLEMENTED  -------------------------
	
	// TODO: Implement
	override def apply(model: ModelLike[Property]): Try[Reply] = ???
}

/**
  * Represents a reply from an LLM
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
// TODO: Implement with specific properties
case class Reply()
