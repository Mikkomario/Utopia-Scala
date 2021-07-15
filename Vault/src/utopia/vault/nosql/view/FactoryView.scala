package utopia.vault.nosql.view

import utopia.vault.nosql.factory.FromResultFactory

/**
  * A common trait for database views that utilize a factory class
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  * @tparam A Type of items accessible through the factory
  */
trait FactoryView[+A] extends View
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The factory used by this view
	  */
	def factory: FromResultFactory[A]
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = factory.table
	
	override def target = factory.target
}
