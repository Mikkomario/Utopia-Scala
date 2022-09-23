package utopia.flow.generic
import utopia.flow.collection.template.typeless.{Model, Property}
import utopia.flow.collection.value.typeless.Value
import utopia.flow.datastructure.template.Model

/**
  * A common trait for factories which process model data, but which can also provide a backup in
  * case model parsing fails
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.16
  */
trait FromModelFactoryWithDefault[+A] extends FromModelFactory[A] with FromValueFactory[A]
{
	// IMPLEMENTED  -----------------------
	
	override def fromValue(value: Value) = value.model.flatMap { apply(_).toOption }
	
	
	// OTHER    --------------------------
	
	/**
	  * Parses an item from the specified model. Returns the default result if parsing fails.
	  * @param model A model from which an item should be parsed.
	  * @return The parsed item (or the default result)
	  */
	def getFromModel(model: Model[Property]) = apply(model).getOrElse(default)
}
