package utopia.logos.model.template

object Placed
{
	/**
	 * Ordering used for placing these items based on their order index
	 */
	implicit val ordering: Ordering[Placed] = Ordering.by { _.orderIndex }
}

/**
 * Common trait for items that may be placed within ordered sequences
 * @author Mikko Hilpinen
 * @since 17.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
 */
trait Placed
{
	/**
	 * @return Index that determines the position of this item (0-based)
	 */
	def orderIndex: Int
}
