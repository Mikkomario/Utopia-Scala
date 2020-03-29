package utopia.vault.model.mutable

import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.sql._

/**
* Readable instances can be read / updated from the database. This trait is designed to be used
* with tables that use primary key indexing. Readable classes are mutable.
* @author Mikko Hilpinen
* @since 25.5.2018
**/
trait Readable extends Storable
{
    // ABSTRACT    ---------------------
    
    /**
     * Updates data in this object. This object may not use all of the data in the update
     * @param data the updated properties
     */
	def set(data: template.Model[Property]): Unit
	
	
	// COMPUTED    ---------------------
	
	/**
	 * Updates the index of this object
	 */
	def index_=(index: Value) = table.primaryColumn.foreach { c => set(c.name, index) }
	
	
	// OTHER    ------------------------
	
	/**
	 * Updates a single property in this object
	 * @param propertyName the name of the property
	 * @param value the new value for the property
	 */
	def set(propertyName: String, value: Value): Unit = set(Model(Vector(propertyName -> value)))
	
	/**
	 * Updates this object based on the current database state. Requires an index
	 * @return Whether any data was read
	 */
	def pull()(implicit connection: Connection): Boolean = pull(SelectAll(table))
	
	/**
	 * Updates this object based on the current database state. Only updates certain
	 * properties. Requires an index
	 * @return Whether any data was read
	 */
	def pull(firstPropName: String, morePropNames: String*)(implicit connection: Connection): Boolean =
	    pull(Select(table, firstPropName, morePropNames: _*))
	
	/**
	 * Updates data in this object, then updates the database as well. Only works when this object
	 * has an index
	 */
	def setAndUpdate(data: Model[Constant])(implicit connection: Connection) =
	{
	    set(data)
	    updateProperties(data.attributeNames)
	}
	
	/**
	 * Updates data in this object, then pushes all data to the database. May insert a new row.
	 * May update the index for this object.
	 */
	def setAndPush(data: template.Model[Property], writeNulls: Boolean = false)(implicit connection: Connection) =
	{
	    set(data)
	    // Updates the index as well
	    index = push(writeNulls)
	}
	
	private def pull(select: SqlSegment)(implicit connection: Connection) =
	{
	    val data = indexCondition.flatMap { c => connection(select + Where(c) + Limit(1)).firstModel }
	    data.foreach(set)
	    data.isDefined
	}
}
