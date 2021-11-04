package utopia.vault.nosql.factory.multi

import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.vault.nosql.factory.CombiningFactoryLike

/**
  * This factory class attaches multiple child instances to a parent instance by utilizing two other factory classes
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.8
  * @tparam Combined The combined result read from this factory
  * @tparam Parent   The parent instance type
  * @tparam Child    The child instance type
  */
trait MultiCombiningFactory[+Combined, Parent, Child]
	extends MultiLinkedFactory[Combined, Child] with CombiningFactoryLike[Combined, Parent, Child]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @param parent   A parent item
	  * @param children Child items
	  * @return A combination of these items
	  */
	def apply(parent: Parent, children: Vector[Child]): Combined
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(id: Value, model: Model, children: Vector[Child]) =
		parentFactory(model).map { p => apply(p, children) }
}
