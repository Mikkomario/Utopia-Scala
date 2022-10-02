package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.time.Now

import scala.util.Success

object OrganizationData extends FromModelFactory[OrganizationData]
{
	override def apply(model: ModelLike[Property]) =
		Success(OrganizationData(model("creator_id"), model("created")))
}

/**
  * Represents an organization or a user group
  * @param creatorId Id of the user who created this organization (if still known)
  * @param created Time when this Organization was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationData(creatorId: Option[Int] = None, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("creator_id" -> creatorId, "created" -> created))
}

