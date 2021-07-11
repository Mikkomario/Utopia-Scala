package utopia.vault.nosql.access.many.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * A common trait for access points which return multiple models at a time
  * and allow accessing via a set of row indices
  * @author Mikko Hilpinen
  * @since 3.4.2021, v1.6.1
  */
trait ManyModelAccessById[+A, -ID] extends ManyModelAccess[A] with UnconditionalView with Indexed
{
	// ABSTRACT	-----------------------
	
	/**
	  * Converts an id to a value
	  * @param id An id to convert
	  * @return The id converted to a value
	  */
	def idToValue(id: ID): Value
	
	
	// OTHER    ----------------------
	
	/**
	  * @param ids Ids to target
	  * @return An access point to models with those ids
	  */
	def apply(ids: Iterable[ID]) = new ManyIdModelAccess[A](ids.map(idToValue), factory)
}
