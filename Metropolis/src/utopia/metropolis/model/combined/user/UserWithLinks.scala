package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactory, ModelConvertible, VectorType}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.Extender
import utopia.metropolis.model.stored.user.{User, UserLanguage}

object UserWithLinks extends FromModelFactory[UserWithLinks]
{
	private val schema = ModelDeclaration(PropertyDeclaration("languages", VectorType))
	
	override def apply(model: Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		User(valid).flatMap { user =>
			valid("languages").getVector.tryMap { v => UserLanguage(user.id, v.getModel) }.map { languages =>
				val deviceIds = model("device_ids").getVector.flatMap { _.int }
				UserWithLinks(user, languages, deviceIds)
			}
		}
	}
}

/**
  * This user model contains links to known languages and used devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param base Standard user data
  * @param languages Languages known to the user, with proficiency levels
  * @param deviceIds Ids of the devices known to the user
  */
case class UserWithLinks(base: User, languages: Vector[UserLanguage], deviceIds: Vector[Int])
	extends Extender[User] with ModelConvertible
{
	override def wrapped = base
	
	override def toModel =
	{
		// Adds additional data to the standard model
		base.toModel + Constant("languages", languages.map { _.toModelWithoutUser }) + Constant("device_ids", deviceIds)
	}
}
