package utopia.metropolis.model.partial.user

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.time.Now

import java.time.Instant

object UserSettingsData extends FromModelFactoryWithSchema[UserSettingsData]
{
	override def schema = ModelDeclaration("name" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		UserSettingsData(model("name").getString, model("email").string)
}

/**
  * Contains basic data about a user
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param name user-name
  * @param email User's email address (if specified)
 *  @param created Creation time of this settings version
  */
case class UserSettingsData(name: String, email: Option[String] = None, created: Instant = Now) extends ModelConvertible
{
	// COMPUTED -------------------------------------
	
	/**
	  * @return Whether this data contains an email address
	  */
	def specifiesEmail = email.nonEmpty
	
	
	// IMPLEMENTED  ---------------------------------
	
	override def toModel = Model(Vector("name" -> name, "email" -> email, "created" -> created))
}
