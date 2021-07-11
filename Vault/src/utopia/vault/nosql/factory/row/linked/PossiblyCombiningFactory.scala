package utopia.vault.nosql.factory.row.linked

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.CombiningFactoryLike

/**
  * A common trait for factory classes that attach a child instance to a parent instance when one exists,
  * but also handle / expect cases where there are no child items to attach
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.8
  */
trait PossiblyCombiningFactory[+Combined, Parent, Child]
	extends PossiblyLinkedFactory[Combined, Child] with CombiningFactoryLike[Combined, Parent, Child]
{
	// ABSTRACT ---------------------------------
	
	/**
	  * Combines a parent and a possible child item
	  * @param parent A parent item
	  * @param child  A possible child item
	  * @return Combined item
	  */
	def apply(parent: Parent, child: Option[Child]): Combined
	
	
	// IMPLEMENTED  -----------------------------
	
	override def apply(model: Model[Constant], child: Option[Child]) =
		parentFactory(model).map { apply(_, child) }
}
