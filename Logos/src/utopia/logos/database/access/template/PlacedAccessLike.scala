package utopia.logos.database.access.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.model.template.PlacedFactory
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.view.FilterableView

/**
 * Common trait for access points for items which may be placed to specific positions within their context.
 * @tparam Repr Implementing access type
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
trait PlacedAccessLike[+Repr] extends FilterableView[Repr] with PlacedFactory[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Column that contains the item's ordering index within its context
	 */
	protected def orderIndexColumn: Column
	
	
	// IMPLEMENTED  --------------------
	
	override def at(orderIndex: Int): Repr = filter(orderIndexColumn <=> orderIndex)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param orderIndex Largest included order index
	 * @return Access to items that come before that index, including that index itself
	 */
	def to(orderIndex: Int) = filter(orderIndexColumn <= orderIndex)
	/**
	 * @param orderIndex Maximum order index (exclusive)
	 * @return Access to items that come before the specified index
	 */
	def until(orderIndex: Int) = filter(orderIndexColumn < orderIndex)
	/**
	 * @param orderIndex Smallest included order index
	 * @return Access to items that come after the specified index, including that index itself
	 */
	def from(orderIndex: Int) = filter(orderIndexColumn >= orderIndex)
	/**
	 * @param orderIndex Minimum order index (exclusive)
	 * @return Access to items that come after the specified index
	 */
	def after(orderIndex: Int) = filter(orderIndexColumn > orderIndex)
}
