package utopia.flow.parse.file.container

import utopia.flow.collection.template.MapLike
import utopia.flow.parse.file.container.SaveTiming.Immediate
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import java.nio.file.Path
import scala.concurrent.ExecutionContext

/**
  * A file-based container which stores a map of objects
  * @author Mikko Hilpinen
  * @since 29.6.2022, v1.16
  */
class ObjectMapFileContainer[A <: ModelConvertible](fileLocation: Path, factory: FromModelFactory[A],
                                                    saveLogic: SaveTiming = Immediate)
                                                   (implicit jsonParser: JsonParser, exc: ExecutionContext, logger: Logger)
	extends FileContainer[Map[String, A]](fileLocation) with MapLike[String, A]
{
	// INITIAL CODE	---------------------------
	
	setupAutoSave(saveLogic)
	
	
	// IMPLEMENTED  ---------------------------
	
	override protected def empty = Map()
	
	override protected def toValue(item: Map[String, A]) =
		Model.withConstants(item.map { case (key, obj) => Constant(key, obj.toModel) })
	override protected def fromValue(value: Value) =
		value.getModel.attributes.flatMap { a => a.value.model.flatMap { factory(_).toOption }.map { a.name -> _ } }
			.toMap
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param key A key
	  * @return A stored object matching that key
	  */
	def apply(key: String) = current(key)
	/**
	  * @param key A key
	  * @return A stored object matching that key, or None
	  */
	def get(key: String) = current.get(key)
	
	/**
	  * Stores or updates an object in this map
	  * @param key Key to which the object is stored
	  * @param obj The object to store
	  */
	def update(key: String, obj: A) = pointer.update { _ + (key -> obj) }
}
