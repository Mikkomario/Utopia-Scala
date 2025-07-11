package utopia.logos.database.access.url.link

import utopia.flow.generic.casting.BasicValueCaster
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.LinkDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual link values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessLinkValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing link database properties
	  */
	val model = LinkDbModel
	
	/**
	  * Access to link id
	  */
	lazy val id = apply(model.index).optional { _.int }
	/**
	  * Id of the targeted internet address, including the specific sub-path
	  */
	lazy val pathId = apply(model.pathId).optional { v => v.int }
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
	lazy val created = apply(model.created).optional { v => v.instant }
}

