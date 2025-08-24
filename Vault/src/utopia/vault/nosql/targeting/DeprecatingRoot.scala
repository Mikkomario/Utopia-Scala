package utopia.vault.nosql.targeting

import utopia.flow.collection.immutable.Pair
import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.targeting.many.ManyDeprecatingRoot
import utopia.vault.nosql.targeting.one.OneDeprecatingRoot
import utopia.vault.nosql.view.{DeprecatableView, FilterableView}

object DeprecatingRoot
{
	// COMPUTED -------------------------
	
	/**
	 * @return Constructors for creating access into individual items
	 */
	def one = OneDeprecatingRoot
	/**
	 * @return Constructors for creating access into multiple items at once
	 */
	def many = ManyDeprecatingRoot
	
	
	// NESTED   -------------------------
	
	/**
	 * Common trait for factories that apply deprecation conditions to filterable (root) views,
	 * converting them to deprecating root interfaces.
	 * @tparam Root Type of (deprecating) root interfaces yielded by this factory
	 */
	trait DeprecatingRootFactory[+Root[X <: FilterableView[X]]]
	{
		// ABSTRACT ---------------------
		
		/**
		 * @param all Access to all elements, including historical ones
		 * @param conditions Applied deprecation & active conditions
		 * @tparam A Type of yielded access points
		 * @return A root level access, which considers the specified conditions when targeting
		 *         active and/or deprecated items.
		 */
		protected def _apply[A <: FilterableView[A]](all: A, conditions: Deprecates): Root[A]
		
		
		// OTHER    -------------------------
		
		/**
		 * @param all Access to all elements, including historical ones
		 * @tparam A Type of yielded access points
		 * @return A root level access which wraps the specified access
		 */
		def apply[A <: DeprecatableView[A]](all: A): Root[A] = _apply(all, all.model)
		/**
		 * @param all Access to all elements, including historical ones
		 * @param conditions Applied deprecation & active conditions
		 * @param alternativeConditions Alternative deprecation conditions (optional)
		 * @tparam A Type of the accessed view
		 * @return A root level access, which considers the specified conditions when targeting
		 *         active and/or deprecated items.
		 */
		def apply[A <: FilterableView[A]](all: A, conditions: Deprecates,
		                                  alternativeConditions: Deprecates*): Root[A] =
		{
			val appliedConditions = all match {
				case d: DeprecatableView[_] => Deprecates.combine(Pair(d.model, conditions).distinct ++ alternativeConditions)
				case _ => Deprecates.combine(conditions +: alternativeConditions)
			}
			_apply[A](all, appliedConditions)
		}
	}
}

/**
 * Common trait for root level access points that can target active and/or historical items separately or together.
 *
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait DeprecatingRoot[+A <: FilterableView[A]]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Access to both active and historical items
	 */
	def all: A
	
	/**
	 * @return A model that determines the active & historical state
	 */
	def model: Deprecates
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Access limited to active items
	 */
	def active = all.filter(model.activeCondition)
	/**
	 * @return Access limited to historical items
	 */
	def historical = all.filter(model.deprecatedCondition)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param condition A condition that, if met, causes the resulting access to be limited to active entries
	 * @return Access to either all entries, or only active entries
	 */
	def activeIf(condition: Boolean) = if (condition) active else all
	/**
	 * @param condition A condition that, if met, causes the resulting access to be limited to historical entries
	 * @return Access to all entries, or only historical entries
	 */
	def historicalIf(condition: Boolean) = if (condition) historical else all
	/**
	 * @param condition A condition that, if met,
	 *                  causes the resulting access to include historical entries, in addition to active ones
	 * @return Access to either all or only active entries
	 */
	def includingHistoryIf(condition: Boolean) = activeIf(!condition)
}
