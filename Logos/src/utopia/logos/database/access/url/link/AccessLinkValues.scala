package utopia.logos.database.access.url.link

import utopia.flow.generic.casting.BasicValueCaster
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.LinkDbModel
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Used for accessing link values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessLinkValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing link database properties
	  */
	val model = LinkDbModel
	
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * Id of the targeted internet address, including the specific sub-path
	  */
	lazy val pathIds = apply(model.pathId) { v => v.getInt }
	/**
	  * Specified request parameters in model format
	  */
	lazy val queryParameters = apply(model.queryParameters) { v =>
		v.notEmpty.flatMap { v => BasicValueCaster.jsonParser(v.getString).toOption } match {
			case Some(v) => v.getModel
			case None => Model.empty
		}
	}
	/**
	  * Time when this link was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

