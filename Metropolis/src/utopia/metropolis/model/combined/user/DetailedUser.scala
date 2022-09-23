package utopia.metropolis.model.combined.user

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.user.UserSettings

/**
  * Combines user information (user settings & user languages)
  * @author Mikko Hilpinen
  * @since 19.2.2022, v2.1
  */
case class DetailedUser(settings: UserSettings, languages: Vector[DetailedUserLanguage])
	extends Extender[UserSettingsData] with ModelConvertible with DescribedSimpleModelConvertible
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Id of this user
	  */
	def id = settings.userId
	
	
	// IMPLEMENTED  ---------------------------
	
	override def wrapped = settings.data
	
	override def toModel =
		Model(Vector("id" -> id, "settings" -> settings.toModel, "languages" -> languages.map { _.toModel }))
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		Model(Vector("id" -> id, "name" -> wrapped.name, "email" -> wrapped.email,
			"languages" -> languages.map { _.toSimpleModelUsing(descriptionRoles) }))
}
