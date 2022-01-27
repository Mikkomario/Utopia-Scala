package utopia.vault.model.immutable

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.row.FromRowFactory

/**
  * These storable instances have a StorableFactory associated with them
  * @author Mikko Hilpinen
  * @since 10.7.2019, v1.2+
  */
trait StorableWithFactory[+Repr] extends Storable
{
	// ABSTRACT	-------------------
	
	/**
	  * @return A factory used by this storable instance
	  */
	def factory: FromRowFactory[Repr]
	
	
	// IMPLEMENTED	---------------
	
	override def table = factory.table
	
	
	// OTHER	-------------------
	
	/**
	  * Searches for a row by using this storable instance as the search condition
	  * @param connection A database connection (implicit)
	  * @return An object from the database, if one could be found
	  */
	def search()(implicit connection: Connection) = factory.find(toCondition)
	
	/**
	  * Searches for multiple rows using this storable instance as the search condition
	  * @param connection A database connection (implicit)
	  * @return Objects from the database matching this condition
	  */
	def searchMany()(implicit connection: Connection) = factory.findMany(toCondition)
	
	/**
	  * Returns the 'maximum' row based on the provided order property
	  * @param orderPropertyName Name of property on which ordering is based
	  * @param connection Database connection
	  */
	def searchMax(orderPropertyName: String)(implicit connection: Connection) = factory.maxBy(orderPropertyName)
	
	/**
	  * Returns the 'minimum' row based on the provided order property
	  * @param orderPropertyName Name of property on which ordering is based
	  * @param connection Database connection
	  */
	def searchMin(orderPropertyName: String)(implicit connection: Connection) = factory.minBy(orderPropertyName)
}
