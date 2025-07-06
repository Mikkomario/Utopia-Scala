package utopia.vault.test.database.access.item.versioned

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vault.test.database.storable.item.VersionedTestItemDbModel

/**
  * Used for accessing individual versioned test item values from the DB
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class AccessVersionedTestItemValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing versioned test item database properties
	  */
	val model = VersionedTestItemDbModel
	
	/**
	  * Access to versioned test item id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Name of this test item
	  */
	lazy val name = apply(model.name) { v => v.getString }
	
	/**
	  * Time when this versioned test item was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	
	/**
	  * Time when this versioned test item became deprecated. None while this versioned test item is 
	  * still valid.
	  */
	lazy val deprecatedAfter = apply(model.deprecatedAfter).optional { v => v.instant }
}

