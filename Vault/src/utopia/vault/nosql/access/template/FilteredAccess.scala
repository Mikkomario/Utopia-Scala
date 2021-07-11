package utopia.vault.nosql.access.template

import utopia.vault.sql.Condition

/**
  * A common trait for access points that provide access to a subset of another access point
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait FilteredAccess[+A] extends Access[A]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return The access point that provides access to unfiltered content
	  */
	protected def parent: Access[_]
	/**
	  * @return A condition to apply over the parent access point's condition
	  */
	def filterCondition: Condition
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table = parent.table
	
	override def globalCondition = Some(parent.mergeCondition(filterCondition))
}
