package utopia.flow.parse.file.container

import utopia.flow.collection.value.typeless.Value

import java.nio.file.Path
import utopia.flow.parse.file.container.SaveTiming.Immediate
import utopia.flow.datastructure.immutable.Value
import utopia.flow.error.DataTypeException
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger

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
						 (implicit jsonParser: JsonParser, exc: ExecutionContext, logger: Logger)
	extends MultiFileContainer[Model](fileLocation)
{
	// INITIAL CODE	-----------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def itemToValue(item: Model) = item
	
	override protected def itemFromValue(value: Value) = value.model.toTry {
		DataTypeException(s"Can't parse ${value.description} to model") }
}
