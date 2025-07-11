package utopia.logos.database.access.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.StatementDbModel
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Used for accessing statement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessStatementValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing statement database properties
	  */
	val model = StatementDbModel
	
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character.
	  */
	lazy val delimiterIds = apply(model.delimiterId) { v => v.int }
	
	/**
	  * Time when this statement was first made
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

