package utopia.vault.nosql.storable

import utopia.flow.collection.value.typeless.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Insert

/**
  * Common trait for database interaction models / access points which allow insertion of model data
  * @author Mikko Hilpinen
  * @since 30.4.2021, v1.7.1
  * @tparam DbModel  Model class used in database interactions
  * @tparam Complete Class that represents an instance that hase been stored to DB
  * @tparam Data     Class that represents an instance before it has been stored to the DB
  */
trait DataInserter[+DbModel <: Storable, +Complete, -Data] extends Indexed
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param data Model data
	  * @return A model matching that data
	  */
	def apply(data: Data): DbModel
	
	/**
	  * @param id   Database id
	  * @param data Inserted data
	  * @return Complete model with id included
	  */
	protected def complete(id: Value, data: Data): Complete
	
	
	// OTHER    ---------------------------
	
	/**
	  * Inserts a new item to the database
	  * @param data       Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Inserted item
	  */
	def insert(data: Data)(implicit connection: Connection) = complete(apply(data).insert(), data)
	
	/**
	  * Inserts multiple new items to the database
	  * @param data       Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Inserted items
	  */
	def insert(data: Seq[Data])(implicit connection: Connection) =
	{
		val ids = Insert(table, data.map { apply(_).toModel }).generatedKeys
		ids.zip(data).map { case (id, data) => complete(id, data) }
	}
}
