package utopia.flow.container

import utopia.flow.collection.value.typeless.Value
import utopia.flow.container.SaveTiming.Immediate
import utopia.flow.generic.ValueConvertible
import utopia.flow.parse.JsonParser
import utopia.flow.util.logging.Logger

import java.nio.file.Path
import scala.concurrent.ExecutionContext

/**
  * A file container implementation that stores a value-convertible item
  * @author Mikko Hilpinen
  * @since 19.8.2022, v1.17
  */
class ValueConvertibleFileContainer[A](fileLocation: Path, saveStyle: SaveTiming = Immediate)
                                      (implicit exc: ExecutionContext, jsonParser: JsonParser,
                                       logger: Logger, toValueConversion: A => ValueConvertible,
                                       fromValueConversion: Value => A)
	extends FileContainer[A](fileLocation)
{
	// INITIAL CODE -----------------------
	
	setupAutoSave(saveStyle)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def empty = fromValue(Value.empty)
	
	override protected def toValue(item: A) = item.toValue
	override protected def fromValue(value: Value) = fromValueConversion(value)
}
