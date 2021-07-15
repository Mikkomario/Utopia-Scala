package utopia.vault.nosql.view

import utopia.vault.sql.Condition

/**
  * A common trait for access points that provide access to a subset of another access point
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait SubView extends View
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return The access point that provides access to unfiltered content
	  */
	protected def parent: View
	
	/**
	  * @return A condition to apply over the parent access point's condition
	  */
	def filterCondition: Condition
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return (Global) condition used by this access point (always defined)
	  */
	def condition = parent.mergeCondition(filterCondition)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table = parent.table
	
	override def target = parent.target
	
	override def globalCondition = Some(condition)
}
