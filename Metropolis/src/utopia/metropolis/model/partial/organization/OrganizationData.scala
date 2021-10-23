package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now

import scala.util.Success

object OrganizationData extends FromModelFactory[OrganizationData]
{
	override def apply(model: template.Model[Property]) =
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

