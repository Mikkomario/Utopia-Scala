package utopia.flow.container

import java.nio.file.Path

import utopia.flow.container.SaveTiming.Immediate
import utopia.flow.datastructure.immutable.Value
import utopia.flow.parse.JsonParser

import scala.concurrent.ExecutionContext

/**
  * A locally backed container that keeps track of a single value
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  * @param fileLocation Location in the file system where this container's back up file should be located
  * @param saveStyle The logic this container should use to save its current value locally
  *                  (default = save whenever value changes)
  * @param jsonParser A parser used for handling json reading
  * @param exc Implicit execution context (used in some saving styles)
  */
class ValueFileContainer(fileLocation: Path, saveStyle: SaveTiming = Immediate)
						(implicit exc: ExecutionContext, jsonParser: JsonParser)
	extends FileContainer[Value](fileLocation)
{
	// INITIAL CODE	------------------------------
	
	setupAutoSave(saveStyle)
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def toValue(item: Value) = item
	
	override protected def fromValue(value: Value) = value
	
	override protected def empty = Value.empty
}
