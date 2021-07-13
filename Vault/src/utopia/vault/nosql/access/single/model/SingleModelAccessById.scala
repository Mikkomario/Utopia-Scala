package utopia.vault.nosql.access.single.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.single.model.distinct.SingleIdModelAccess
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
 * Used as a top-level accessor that provides access to individual models by searching with their ids
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleModelAccessById[+A, -ID] extends SingleModelAccess[A] with UnconditionalView with Indexed
{
	// ABSTRACT	-----------------------
	
	/**
	 * Converts an id to a value
	 * @param id An id to convert
	 * @return The id converted to a value
	 */
	def idToValue(id: ID): Value
	
	
	// OTHER	-----------------------
	
	/**
	 * Provides access to an individual model with an id
	 * @param id An id
	 * @return An access point to a model with that id
	 */
	def apply(id: ID): SingleIdModelAccess[A] = new SingleIdModelAccess[A](
		idToValue(id), factory)
}

object SingleModelAccessById
{
	// OTHER	----------------------
	
	/**
	 * Creates a new access point by wrapping a model factory
	 * @param factory A factory
	 * @param idConversion An implicit conversion from specified id type to value
	 * @tparam A Type of model read
	 * @tparam ID Type of id used in searches
	 * @return A new access point
	 */
	def apply[A, ID](factory: FromResultFactory[A])(implicit idConversion: ID => Value): SingleModelAccessById[A, ID] =
		new FactoryWrapper(factory)
	
	
	// NESTED	----------------------
	
	private class FactoryWrapper[+A, -ID](val factory: FromResultFactory[A])(implicit idConversion: ID => Value)
		extends SingleModelAccessById[A, ID]
	{
		override def idToValue(id: ID) = idConversion(id)
	}
}