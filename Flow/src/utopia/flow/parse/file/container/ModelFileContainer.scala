package utopia.flow.parse.file.container

import java.nio.file.Path
import utopia.flow.parse.file.container.SaveTiming.Immediate
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

/**
  * A container with local file backup used for storing a single model
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1.8
  * @param fileLocation A location where the local backup file will be stored
  * @param saveLogic Logic used when saving container contents (default = save whenever content changes)
  * @param jsonParser A parser used for reading stored json data
  * @param exc Execution context, used in some save features
  */
class ModelFileContainer(fileLocation: Path, saveLogic: SaveTiming = Immediate)
						(implicit jsonParser: JsonParser, exc: ExecutionContext, logger: Logger)
	extends FileContainer[Model](fileLocation)
{
	// INITIAL CODE	-------------------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def toValue(item: Model) = item
	
	override protected def fromValue(value: Value) = value.getModel
	
	override protected def empty = Model.empty
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param propertyName Name of a property
	  * @return Currently stored value for that property (empty value if not stored)
	  */
	def apply(propertyName: String) = current(propertyName)
	
	/**
	  * Updates a property in this container
	  * @param propertyName Name of the targeted property
	  * @param newValue A new value for that property
	  */
	def update(propertyName: String, newValue: Value) = current += Constant(propertyName, newValue)
}
