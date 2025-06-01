package utopia.logos.database.access.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Used for accessing delimiter values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDelimiterValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing delimiter database properties
	  */
	val model = DelimiterDbModel
	
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * The characters that form this delimiter
	  */
	lazy val text = apply(model.text) { v => v.getString }
	
	/**
	  * Time when this delimiter was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

