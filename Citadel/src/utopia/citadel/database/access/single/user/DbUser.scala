package utopia.citadel.database.access.single.user

import utopia.citadel.database.access.many.description.{DbLanguageDescriptions, DbLanguageFamiliarityDescriptions}
import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.user.{UserLanguageLinkModel, UserModel, UserSettingsModel}
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.user.{DetailedUser, DetailedUserLanguage}
import utopia.metropolis.model.partial.user.{UserLanguageLinkData, UserSettingsData}
import utopia.metropolis.model.stored.language.{Language, LanguageFamiliarity}
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Users
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUser extends SingleRowModelAccess[User] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserModel
	
	private def settingsModel = UserSettingsModel
	private def languageLinkModel = UserLanguageLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted User instance
	  * @return An access point to that User
	  */
	def apply(id: Int) = DbSingleUser(id)
	
	/**
	  * Inserts a new user to the database. Also inserts user settings, language links and a possible device connection
	  * @param name Name of this user
	  * @param email Email address of this user (optional)
	  * @param languageData Languages known by this user, along with their respective familiarity levels
	  *                     (default = empty)
	  * @param connection Implicit DB Connection
	  * @return Inserted user data
	  */
	def insert(name: String, email: Option[String] = None,
	           languageData: Vector[(Language, LanguageFamiliarity)] = Vector())
	          (implicit connection: Connection) =
	{
		// Inserts new user data
		val user = model.insert()
		val settings = settingsModel.insert(UserSettingsData(user.id, name, email))
		val sortedLanguageData = languageData.sortBy { _._2.orderIndex }
		val languageLinks = languageLinkModel.insert(
			sortedLanguageData
				.map { case (language, familiarity) => UserLanguageLinkData(user.id, language.id, familiarity.id) })
		
		// Reads language and familiarity descriptions
		implicit val languageIds: LanguageIds = LanguageIds(sortedLanguageData.map { _._1.id })
		val languageDescriptions = DbLanguageDescriptions.forPreferredLanguages.inPreferredLanguages
		val familiarityDescriptions = DbLanguageFamiliarityDescriptions(languageLinks.map { _.familiarityId }.toSet)
			.inPreferredLanguages
		val describedLanguagePerId = sortedLanguageData.map { case (language, _) =>
			language.id -> language.withDescriptions(languageDescriptions.getOrElse(language.id, Vector()).toSet)
		}.toMap
		val describedFamiliarityPerId = sortedLanguageData.map { _._2 }.toSet[LanguageFamiliarity].map { f =>
			f.id -> f.withDescriptions(familiarityDescriptions.getOrElse(f.id, Vector()).toSet)
		}.toMap
		
		// Returns inserted user with language data included
		DetailedUser(settings,
			languageLinks.map { link =>
				DetailedUserLanguage(link, describedLanguagePerId(link.languageId),
					describedFamiliarityPerId(link.familiarityId))
			})
	}
}

