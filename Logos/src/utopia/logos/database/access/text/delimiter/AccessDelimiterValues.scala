package utopia.logos.database.access.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing delimiter values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDelimiterValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing delimiter database properties
	  */
	val model = DelimiterDbModel
	
	/**
	  * Access to delimiter ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * The characters that form this delimiter
	  */
	lazy val text = apply(model.text) { v => v.getString }
	/**
	  * Time when this delimiter was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	
	
	// COMPUTED -----------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return A map which converts delimiter IDs to delimiter text values. Contains a default value.
	 */
	def idToText(implicit connection: Connection) =
		access.toMap(ids.column, text.column) { _.getInt } { _.getString }.withDefaultValue("")
}

