package utopia.flow.parse.file.container

import java.nio.file.Path
import utopia.flow.parse.file.container.SaveTiming.Immediate
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

/**
  * Used for storing 0 to 1 instances of an object
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1.8
  * @param fileLocation Location where the object file will be stored
  * @param factory Factory used for parsing object data from a model
  * @param saveLogic Saving logic (default = save whenever content changes)
  * @param jsonParser Json parser used (implicit)
  * @param exc Execution context used in asynchronous saving processes (implicit)
  */
class OptionObjectFileContainer[A <: ModelConvertible](fileLocation: Path, factory: FromModelFactory[A],
													   saveLogic: SaveTiming = Immediate)
													  (implicit jsonParser: JsonParser, exc: ExecutionContext, logger: Logger)
	extends OptionFileContainer[A](fileLocation)
{
	// INITIAL CODE	-----------------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def itemToValue(item: A) = item.toModel
	
	override protected def fromValue(value: Value) = value.model.flatMap { factory(_).toOption }
}
