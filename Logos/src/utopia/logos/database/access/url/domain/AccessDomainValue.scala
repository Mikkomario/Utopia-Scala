package utopia.logos.database.access.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.DomainDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual domain values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDomainValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing domain database properties
	  */
	val model = DomainDbModel
	
	/**
	  * Access to domain id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Full http(s) address of this domain in string format. Includes protocol, domain name and 
	  * possible port number.
	  */
	lazy val url = apply(model.url) { v => v.getString }
	
	/**
	  * Time when this domain was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

