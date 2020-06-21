package utopia.flow.container

import java.nio.file.Path

import utopia.flow.container.SaveTiming.Immediate
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser

import scala.concurrent.ExecutionContext

/**
  * A container that stores a single object which has its own empty status
  * @author Mikko Hilpinen
  * @since 21.6.2020, v1
  * @param fileLocation Location where the object data is stored
  * @param factory A factory used for parsing object data
  * @param saveLogic Logic used regarding status saves (default = save whenever object is changed)
  * @param makeEmpty An empty object (call by name)
  * @param jsonParser Parser used for interpreting json strings (implicit)
  * @param exc Execution context, used in some save methods (implicit)
  */
class ObjectFileContainer[A <: ModelConvertible](fileLocation: Path, factory: FromModelFactory[A],
												 saveLogic: SaveTiming = Immediate)(makeEmpty: => A)
												(implicit jsonParser: JsonParser, exc: ExecutionContext)
	extends FileContainer[A](fileLocation)
{
	// INITIAL CODE	---------------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def toValue(item: A) = item.toModel
	
	// Errors here are ignored for now
	override protected def fromValue(value: Value) = value.model.flatMap { factory(_).toOption }.getOrElse(makeEmpty)
	
	override protected def empty = makeEmpty
}
