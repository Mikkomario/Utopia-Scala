package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.view.FilterableView

import scala.language.implicitConversions

/**
 * Provides access to multiple row-based items in the database at once.
 * Expects the targeted items to be separated into active and deprecated instances.
 * @tparam A Type of access points yielded
 * @param defaultDeprecation Active / deprecation conditions that are applicable to all generated access points
 * @author Mikko Hilpinen
 * @since 19.08.2025, v2.0
 */
abstract class DeprecatingWrapRowAccess[A[X] <: FilterableView[A[X]]](defaultDeprecation: Deprecates)
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Access to root access point factories, which apply the default filtering between active & historical items
	 */
	lazy val roots = new Roots(defaultDeprecation)
	
	
	// ABSTRACT ----------------------------
	
	/**
	 * @param access An access point to wrap
	 * @tparam I Type of accessed / pulled items
	 * @return An access point wrapping that access
	 */
	protected def wrap[I](access: TargetingManyRows[I]): A[I]
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param reader A database row reader to wrap
	 * @tparam I Type of individual accessed items
	 * @return A root-level access wrapping the specified reader
	 */
	def apply[I](reader: DbRowReader[I]): AccessManyDeprecatingRoot[A[I]] = roots(reader)
	/**
	 * @param all Access to all items, whether active or historical
	 * @tparam I Type of individual accessed items
	 * @return A root-level access wrapping the specified access point
	 */
	def apply[I](all: TargetingManyRows[I]): AccessManyDeprecatingRoot[A[I]] = roots(all)
	
	/**
	 * @param reader A database row reader to wrap
	 * @param additionalConditions Deprecation / active conditions to apply
	 *                             in addition to those specified by this factory
	 * @tparam I Type of individual accessed items
	 * @return A root-level access wrapping the specified reader
	 */
	def apply[I](reader: DbRowReader[I], additionalConditions: Deprecates): AccessManyDeprecatingRoot[A[I]] =
		apply(additionalConditions)(reader)
	/**
	 * @param all Access to all items, whether active or historical
	 * @param additionalConditions Deprecation / active conditions to apply
	 *                             in addition to those specified by this factory
	 * @tparam I Type of individual accessed items
	 * @return A root-level access wrapping the specified access point
	 */
	def apply[I](all: TargetingManyRows[I], additionalConditions: Deprecates): AccessManyDeprecatingRoot[A[I]] =
		apply(additionalConditions)(all)
	
	/**
	 * @param otherDeprecation Additional deprecation / active conditions to apply
	 * @return Access to constructing root access points, which apply the specified deprecation conditions,
	 *         in addition to the default conditions specified by this factory
	 */
	def apply(otherDeprecation: Deprecates) = roots + otherDeprecation
	
	
	// NESTED   ---------------------------
	
	/**
	 * An interface for generating root-level access points,
	 * which apply some filtering between active and deprecated items
	 * @param conditions The conditions that determine, which items are active and which are deprecated
	 */
	class Roots(conditions: Deprecates) extends WrapRowAccess[Root]
	{
		// IMPLEMENTED  -------------------
		
		override def apply[I](access: TargetingManyRows[I]): Root[I] = Root(wrap(access), conditions)
		
		
		// OTHER    -----------------------
		
		/**
		 * @param conditions Additional active / deprecated conditions to apply
		 * @return A copy of this factory, also applying the specified conditions
		 */
		def +(conditions: Deprecates) = new Roots(Deprecates.combine(this.conditions, conditions))
		/**
		 * @param conditions Additional active / deprecated conditions to apply
		 * @return A copy of this factory, also applying the specified conditions
		 */
		def ++(conditions: IterableOnce[Deprecates]) = {
			val allConditions = Single(this.conditions) ++ conditions
			if (allConditions.hasSize(1))
				this
			else
				new Roots(Deprecates.combine(allConditions))
		}
	}
	/**
	 * @param all Provides access to all items, whether active or historical
	 * @param model A model that determines which items are to be considered active and which deprecated
	 * @tparam I Type of individual accessed items
	 */
	case class Root[I](all: A[I], model: Deprecates) extends AccessManyDeprecatingRoot[A[I]]
}
