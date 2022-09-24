package utopia.flow.parse.file.container

import utopia.flow.parse.file.container.SaveTiming.Immediate
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import java.nio.file.Path
import scala.concurrent.ExecutionContext

/**
  * A file container implementation that stores a value-convertible item as an Option
  * @author Mikko Hilpinen
  * @since 19.8.2022, v1.17
  */
class ValueConvertibleOptionFileContainer[A](fileLocation: Path, saveStyle: SaveTiming = Immediate)
                                            (implicit exc: ExecutionContext, jsonParser: JsonParser,
                                             logger: Logger, toValueConversion: A => ValueConvertible,
                                             fromValueConversion: Value => Option[A])
	extends FileContainer[Option[A]](fileLocation)
{
	// INITIAL CODE -----------------------
	
	setupAutoSave(saveStyle)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def empty = None
	
	override protected def toValue(item: Option[A]) = item match {
		case Some(a) => a.toValue
		case None => Value.empty
	}
	override protected def fromValue(value: Value) = fromValueConversion(value)
}
