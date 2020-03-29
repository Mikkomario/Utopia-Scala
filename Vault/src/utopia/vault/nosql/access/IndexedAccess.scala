package utopia.vault.nosql.access

/**
 * A common trait for access points that use indexed tables
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 * @tparam A Type of item retrieved through this access point
 */
trait IndexedAccess[+A] extends Access[A]
{
	// COMPUTED	-----------------------
	
	/**
	 * @return The index column in the primary table
	 */
	def index = table.primaryColumn.get
}
