package utopia.logos.database.access.url.path

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual request path values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessRequestPathValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing request path database properties
	  */
	val model = RequestPathDbModel
	
	/**
	  * Access to request path id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Id of the domain part of this url
	  */
	lazy val domainId = apply(model.domainId).optional { v => v.int }
	
	/**
	  * Part of this url that comes after the domain part. Doesn't include any query parameters, nor 
	  * the initial forward slash.
	  */
	lazy val path = apply(model.path) { v => v.getString }
	
	/**
	  * Time when this request path was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

