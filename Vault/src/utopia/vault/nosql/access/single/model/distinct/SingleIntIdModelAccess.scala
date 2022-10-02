package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.factory.FromResultFactory

object SingleIntIdModelAccess
{
	// OTHER    -----------------------------
	
	/**
	 * @param id Targeted item's id
	 * @param factory Factory used for accessing that item
	 * @tparam A Type of accessed item
	 * @return An access point to that item's information
	 */
	def apply[A](id: Int, factory: FromResultFactory[A]): SingleIntIdModelAccess[A] =
		new SimpleSingleIntIdModelAccess[A](id, factory)
	
	
	// NESTED   -----------------------------
	
	private class SimpleSingleIntIdModelAccess[+A](override val id: Int, override val factory: FromResultFactory[A])
		extends SingleIntIdModelAccess[A]
}

/**
 * A common trait for access points that target a single database item based on its primary row id,
 * which is of type int
 * @author Mikko Hilpinen
 * @since 13.10.2021, v1.11
 */
trait SingleIntIdModelAccess[+A] extends SingleIdModelAccess[A]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Database row id of the targeted item
	 */
	def id: Int
	
	
	// IMPLEMENTED  ------------------------
	
	override def idValue = id
}
