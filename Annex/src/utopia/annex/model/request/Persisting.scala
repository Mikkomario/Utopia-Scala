package utopia.annex.model.request

import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.template.eventful.Changing

/**
  * Common trait for items that may be persisted between sessions in json/model form
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.7
  */
trait Persisting
{
	/**
	  * @return A pointer that contains the model that can be stored locally to replicate this item in another session.
	  *         Contains None if this item shouldn't be persisted.
	  */
	def persistingModelPointer: Changing[Option[Model]]
}
