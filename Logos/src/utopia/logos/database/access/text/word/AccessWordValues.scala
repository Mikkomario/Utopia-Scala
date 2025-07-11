package utopia.logos.database.access.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.WordDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing word values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWordValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing word database properties
	  */
	val model = WordDbModel
	
	/**
	  * Access to word ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Text representation of this word
	  */
	lazy val text = apply(model.text) { v => v.getString }
	
	/**
	  * Time when this word was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

