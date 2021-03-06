package utopia.flow.container

import java.nio.file.Path

import utopia.flow.container.SaveTiming.Immediate
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.DataTypeException
import utopia.flow.parse.JsonParser
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.ExecutionContext

/**
  * This container holds a number of immutable models
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.8
  * @param fileLocation location where the backup of this container is stored
  * @param saveLogic Logic used when saving the contents of this container (default = save whenever content changes)
  * @param jsonParser parser used when reading json (implicit)
  * @param exc Implicit execution context (used in some auto save logic options)
  */
class ModelsFileContainer(fileLocation: Path, saveLogic: SaveTiming = Immediate)
						 (implicit jsonParser: JsonParser, exc: ExecutionContext)
	extends MultiFileContainer[Model[Constant]](fileLocation)
{
	// INITIAL CODE	-----------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def itemToValue(item: Model[Constant]) = item
	
	override protected def itemFromValue(value: Value) = value.model.toTry {
		DataTypeException(s"Can't parse ${value.description} to model") }
}
