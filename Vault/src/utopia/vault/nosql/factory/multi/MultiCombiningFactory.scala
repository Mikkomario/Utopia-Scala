package utopia.vault.nosql.factory.multi

import utopia.vault.model.immutable.Result
import utopia.vault.nosql.factory.{CombiningFactoryLike, LinkedFactoryLike}

/**
  * This factory class attaches multiple child instances to a parent instance by utilizing two other factory classes
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.8
  * @tparam Combined The combined result read from this factory
  * @tparam Parent   The parent instance type
  * @tparam Child    The child instance type
  */
trait MultiCombiningFactory[+Combined, Parent, Child]
	extends LinkedFactoryLike[Combined, Child] with CombiningFactoryLike[Combined, Parent, Child]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @param parent   A parent item
	  * @param children Child items
	  * @return A combination of these items
	  */
	def apply(parent: Parent, children: Seq[Child]): Combined
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(result: Result): Seq[Combined] = result.group(parentFactory, childFactory)(apply).toSeq
}
