package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.collection.value.typeless.Value
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Indexed

object SingleIdModelAccess
{
	// OTHER    ---------------------------------
	
	/**
	 * Creates a new single model access point based on target item's database id
	 * @param id Id of the targeted item (as a value)
	 * @param factory Factory used for reading the item
	 * @tparam A Type of the item to read
	 * @return A new access point for that item
	 */
	def apply[A](id: Value, factory: FromResultFactory[A]): SingleIdModelAccess[A] =
		new SimpleSingleIdModelAccess[A](id, factory)
	
	
	// NESTED   ---------------------------------
	
	private class SimpleSingleIdModelAccess[+A](override val idValue: Value,
	                                            override val factory: FromResultFactory[A])
		extends SingleIdModelAccess[A]
}

/**
 * A common trait for access points that target a single database item based on its primary index (id)
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleIdModelAccess[+A] extends UniqueModelAccess[A] with Indexed
{
	// ABSTRACT ---------------------------------
	
	/**
	 * @return Targeted row id as a value
	 */
	def idValue: Value
	
	
	// IMPLEMENTED  -----------------------------
	
	override def condition = index <=> idValue
}