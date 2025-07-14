package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.read.DbRowReader

/**
 * Common trait for (root) factory classes which wrap generic access points
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v1.22
 */
trait WrapRowAccess[+A[_]]
{
	// ABSTRACT --------------------------
	
	/**
	 * @param access An access point to wrap
	 * @tparam I Type of accessed / pulled items
	 * @return An access point wrapping that access
	 */
	def apply[I](access: AccessManyRows[I]): A[I]
	
	
	// OTHER    --------------------------
	
	/**
	 * @param reader A row-based DB reader
	 * @tparam I Type of items accessed through the specified reader
	 * @return An access point wrapping / using that reader
	 */
	def apply[I](reader: DbRowReader[I]): A[I] = apply(AccessManyRows(reader))
}
