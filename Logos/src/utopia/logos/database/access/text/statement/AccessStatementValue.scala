package utopia.logos.database.access.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.StatementDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual statement values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessStatementValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing statement database properties
	  */
	val model = StatementDbModel
	
	/**
	  * Access to statement id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character.
	  */
	lazy val delimiterId = apply(model.delimiterId).optional { v => v.int }
	
	/**
	  * Time when this statement was first made
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

