package utopia.vault.nosql.factory.row.linked

import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.CombiningFactoryLike

/**
  * This factory class combines the results of two other factories from a join result
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.8
  * @tparam Combined The combined result read from this factory
  * @tparam Parent   The parent instance type
  * @tparam Child    The child instance type
  */
trait CombiningFactory[+Combined, Parent, Child]
	extends LinkedFactory[Combined, Child] with CombiningFactoryLike[Combined, Parent, Child]
{
	// ABSTRACT --------------------------------
	
	/**
	  * @param parent Parent instance
	  * @param child  Child instance
	  * @return The combination of these two instances
	  */
	def apply(parent: Parent, child: Child): Combined
	
	
	// IMPLEMENTED  ----------------------------
	
	override def apply(model: Model, child: Child) = parentFactory(model).map { apply(_, child) }
}
