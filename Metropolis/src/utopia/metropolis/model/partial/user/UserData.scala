package utopia.metropolis.model.partial.user

import utopia.flow.collection.template.typeless
import utopia.flow.collection.template.typeless.Property
import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.datastructure.template
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

import scala.util.Success

object UserData extends FromModelFactory[UserData]
{
	override def apply(model: typeless.Model[Property]) =
		Success(apply(model("created").getInstant))
}

/**
  * Represents a program user
  * @param created Time when this User was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserData(created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("created" -> created))
}

