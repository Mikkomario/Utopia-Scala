package utopia.annex.model.request
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

/**
  * Common trait for persisting items that don't change their persisted form
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.7
  */
trait ConsistentlyPersisting extends Persisting
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Persisted form of this item. I.e. a model representing this item between sessions.
	  */
	def persistingModel: Model
	
	
	// IMPLEMENTED  --------------------------
	
	override def persistingModelPointer: Changing[Option[Model]] = Fixed(Some(persistingModel))
}
