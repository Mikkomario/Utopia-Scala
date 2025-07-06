package utopia.vault.test.database.access.item.versioned

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues
import utopia.vault.test.database.storable.item.VersionedTestItemDbModel

/**
  * Used for accessing versioned test item values from the DB
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class AccessVersionedTestItemValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing versioned test item database properties
	  */
	val model = VersionedTestItemDbModel
	
	/**
	  * Access to versioned test item ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Name of this test item
	  */
	lazy val names = apply(model.name) { v => v.getString }
	
	/**
	  * Time when this versioned test item was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	
	/**
	  * Time when this versioned test item became deprecated. None while this versioned test item is 
	  * still valid.
	  */
	lazy val deprecationTimes = apply(model.deprecatedAfter).flatten { v => v.instant }
}

