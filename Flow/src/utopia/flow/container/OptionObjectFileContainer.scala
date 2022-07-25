package utopia.flow.container

import java.nio.file.Path
import utopia.flow.container.SaveTiming.Immediate
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.parse.JsonParser
import utopia.flow.generic.ValueConversions._
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
