package utopia.logos.database.access.url.path

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing request path values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessRequestPathValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing request path database properties
	  */
	val model = RequestPathDbModel
	
	/**
	  * Access to request path ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Id of the domain part of this url
	  */
	lazy val domainIds = apply(model.domainId) { v => v.getInt }
	
	/**
	  * Part of this url that comes after the domain part. Doesn't include any query parameters, nor 
	  * the initial forward slash.
	  */
	lazy val paths = apply(model.path) { v => v.getString }
	
	/**
	  * Time when this request path was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

