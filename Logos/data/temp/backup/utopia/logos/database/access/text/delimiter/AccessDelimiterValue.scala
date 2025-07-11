package utopia.logos.database.access.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual delimiter values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDelimiterValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing delimiter database properties
	  */
	val model = DelimiterDbModel
	
	lazy val id = apply(model.index) { _.getInt }
	
	/**
	  * The characters that form this delimiter
	  */
	lazy val text = apply(model.text) { v => v.getString }
	
	/**
	  * Time when this delimiter was added to the database
	  */
	lazy val created = apply(model.created) { v => v.getInstant }
}

