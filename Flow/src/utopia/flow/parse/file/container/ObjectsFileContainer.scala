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
  * Used for storing a number of models of a certain type. Data is backed in a local file.
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  * @param fileLocation Location in the file system where this container's back up file should be located
  * @param factory    A factory used for parsing models into items
  * @param saveLogic  The logic this container should use to save its current value locally
  *                   (default = save whenever value changes)
  * @param jsonParser A parser used for handling json reading
  * @param exc Implicit execution context (used in some saving styles)
  * @tparam A Type of individual items stored in this container
  */
class ObjectsFileContainer[A <: ModelConvertible](fileLocation: Path,
												  val factory: FromModelFactory[A], saveLogic: SaveTiming = Immediate)
												 (implicit jsonParser: JsonParser, exc: ExecutionContext, logger: Logger)
	extends MultiFileContainer[A](fileLocation)
{
	// INITIAL CODE	-------------------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def itemToValue(item: A) = item.toModel
	
	override protected def itemFromValue(value: Value) = factory(value.getModel)
}
