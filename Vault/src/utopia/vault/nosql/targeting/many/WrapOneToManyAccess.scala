package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.read.DbReader

/**
 * Common trait for (root) access interfaces, which wrap other generic (one-to-many) access classes
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v1.22
 */
trait WrapOneToManyAccess[+A[_]]
{
	// ABSTRACT -------------------------
	
	/**
	 * @param access A generic access point to wrap
	 * @tparam I Type of accessed items
	 * @return An access wrapper
	 */
	def apply[I](access: AccessMany[I]): A[I]
	
	
	// OTHER    -------------------------
	
	/**
	 * @param reader A database reader
	 * @tparam I Type of accessed items
	 * @return An access point using that reader
	 */
	def apply[I](reader: DbReader[Seq[I]]): A[I] = apply(AccessMany(reader))
}
