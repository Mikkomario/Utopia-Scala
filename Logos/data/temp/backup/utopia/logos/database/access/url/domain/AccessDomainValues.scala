package utopia.logos.database.access.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.DomainDbModel
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Used for accessing domain values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDomainValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing domain database properties
	  */
	val model = DomainDbModel
	
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Full http(s) address of this domain in string format. Includes protocol, domain name and 
	  * possible port number.
	  */
	lazy val urls = apply(model.url) { v => v.getString }
	
	/**
	  * Time when this domain was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

