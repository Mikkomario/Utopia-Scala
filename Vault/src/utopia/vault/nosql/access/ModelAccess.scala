package utopia.vault.nosql.access

import utopia.vault.nosql.factory.FromResultFactory

/**
 * Common trait for access points that return parsed model data
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 * @tparam M Type of model returned
 * @tparam A The format in which model data is returned (Eg. a list of models)
 */
trait ModelAccess[+M, +A] extends Access[A]
{
	// ABSTRACT	-------------------------
	
	/**
	 * @return The factory used for parsing accessed data
	 */
	def factory: FromResultFactory[M]
	
	
	// IMPLEMENTED	---------------------
	
	final override def table = factory.table
	
	
	// COMPUTED	-------------------------
	
	/**
	 * @return The selection target used
	 */
	def target = factory.target
}
