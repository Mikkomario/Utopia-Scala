package utopia.citadel.database.access.single

import utopia.citadel.database.access.id.single.DbUserId
import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.citadel.database.access.many.language.DbLanguageFamiliarities
import utopia.citadel.database.access.many.organization.{DbUserRoles, InvitationsAccess}
import utopia.citadel.database.factory.organization.{MembershipFactory, MembershipWithRolesFactory}
import utopia.citadel.database.factory.user.{FullUserLanguageFactory, UserFactory, UserLanguageFactory, UserSettingsFactory}
import utopia.citadel.database.model.organization.MembershipModel
import utopia.citadel.database.model.user.{UserDeviceModel, UserLanguageModel, UserSettingsModel}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.language.{DescribedLanguage, DescribedLanguageFamiliarity}
import utopia.metropolis.model.combined.user.{DescribedUserLanguage, MyOrganization}
import utopia.metropolis.model.error.AlreadyUsedException
import utopia.metropolis.model.partial.user.{UserLanguageData, UserSettingsData}
import utopia.metropolis.model.stored.organization.Membership
import utopia.metropolis.model.stored.user.{User, UserLanguage, UserSettings}
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.BasicCombineOperator.Or
import utopia.vault.nosql.access.{ManyModelAccess, SingleIdModelAccess, SingleModelAccess, UniqueIdAccess, UniqueModelAccess}
import utopia.vault.sql.{Delete, Select, Where}
import utopia.vault.sql.SqlExtensions._

import scala.util.{Failure, Success}

/**
  * Used for accessing individual user's data
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbUser extends SingleModelAccess[User]
{
	// IMPLEMENTED	---------------------
	
	override def factory = UserFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param userId Id of targeted user
	  * @return An access point to that user's data
	  */
	def apply(userId: Int) = DbSingleUser(userId)
	
	
	// NESTED	-------------------------
	
	case class DbSingleUser(userId: Int) extends SingleIdModelAccess(userId, DbUser.factory)
	{
		// IMPLEMENTED	-----------------
		
		override val factory = DbUser.factory
		
		
		// COMPUTED	---------------------
		
		/**
		  * @return An access point to this user's known languages
		  */
		def languages = DbUserLanguages
		
		/**
		  * @param connection DB Connection
		  * @return Ids of the languages known by this user
		  */
		def languageIds(implicit connection: Connection) = connection(Select(UserLanguageModel.table,
			UserLanguageModel.languageIdAttName) + Where(UserLanguageModel.withUserId(userId).toCondition)).rowIntValues
		
		/**
		  * @param connection DB Connection
		  * @return Ids of the devices this user has used
		  */
		def deviceIds(implicit connection: Connection) = connection(Select(UserDeviceModel.table,
			UserDeviceModel.deviceIdAttName) + Where(UserDeviceModel.withUserId(userId).toCondition &&
			UserDeviceModel.nonDeprecatedCondition)).rowIntValues
		
		/**
		  * @param connection DB Connection
		  * @return This user's data, along with linked data
		  */
		def withLinks(implicit connection: Connection) =
			pull.map { base => factory.complete(base) }
		
		/**
		  * @return An access point to this user's current settings
		  */
		def settings = DbUserSettings
		
		/**
		  * @param connection DB Connection (implicit), used for reading user email address
		  * @return An access point to invitations for this user
		  */
		// Will need to read settings for accessing since joining logic would get rather complex otherwise
		def receivedInvitations(implicit connection: Connection) = new DbUserInvitations(settings.map { _.email })
		
		/**
		  * @return An access point to this user's memberships
		  */
		def memberships = DbUserMemberships
		
		
		// OTHER	----------------------
		
		/**
		  * @param organizationId Id of the targeted organization
		  * @param connection DB Connection (implicit)
		  * @return Whether this user is a member of the specified organization
		  */
		def isMemberInOrganizationWithId(organizationId: Int)(implicit connection: Connection) =
			membershipIdInOrganizationWithId(organizationId).isDefined
		
		/**
		  * @param organizationId Id of targeted organization
		  * @return An access point to this user's membership id in that organization
		  */
		def membershipIdInOrganizationWithId(organizationId: Int) = DbUserMembershipId(organizationId)
		
		/**
		  * Links this user with the specified device
		  * @param deviceId Id of targeted device (must be valid)
		  * @param connection DB Connection (implicit)
		  * @return Whether a new link was created (false if there already existed a link between this user and the device)
		  */
		def linkWithDeviceWithId(deviceId: Int)(implicit connection: Connection) =
		{
			// Checks whether there already exists a connection between this user and specified device
			if (UserDeviceModel.exists(UserDeviceModel.withUserId(userId).withDeviceId(deviceId).toCondition &&
				UserDeviceModel.nonDeprecatedCondition))
				false
			else
			{
				UserDeviceModel.insert(userId, deviceId)
				true
			}
		}
		
		
		// NESTED	-----------------------
		
		object DbUserSettings extends UniqueModelAccess[UserSettings]
		{
			// IMPLEMENTED	---------------
			
			override def condition = model.withUserId(userId).toCondition && factory.nonDeprecatedCondition
			
			override def factory = UserSettingsFactory
			
			
			// COMPUTED	-------------------
			
			private def model = UserSettingsModel
			
			
			// OTHER	-------------------
			
			/**
			  * Updates this user's current settings
			  * @param newSettings New user settings version
			  * @param connection DB Connection (implicit)
			  * @return Newly inserted settings. Failure if the email address is reserved for another user.
			  */
			def update(newSettings: UserSettingsData)(implicit connection: Connection) =
			{
				// Makes sure the email address is still available (or belongs to this user)
				if (DbUserId.forEmail(newSettings.email).forall { _ == userId })
				{
					// Deprecates the old settings
					model.nowDeprecated.updateWhere(condition)
					// Inserts new settings
					Success(model.insert(userId, newSettings))
				}
				else
					Failure(new AlreadyUsedException(s"Email address ${newSettings.email} is already in use by another user"))
			}
		}
		
		object DbUserLanguages extends ManyModelAccess[UserLanguage]
		{
			// IMPLEMENTED	---------------
			
			override def factory = UserLanguageFactory
			
			override def globalCondition = Some(condition)
			
			override protected def defaultOrdering = None
			
			
			// COMPUTED	-------------------
			
			private def condition = model.withUserId(userId).toCondition
			
			private def model = UserLanguageModel
			
			/**
			  * @param connection DB Connection (implicit)
			  * @return User languages, including language data
			  */
			def full(implicit connection: Connection) = FullUserLanguageFactory.getMany(condition)
			
			/**
			  * @param connection DB Connection (implicit)
			  * @return Ids of the languages known to this user, each paired with this user's familiarity level
			  *         in that language
			  */
			def withFamiliarityLevels(implicit connection: Connection) =
				DbLanguageFamiliarities.familiarityLevelsForUserWithId(userId)
			
			
			// OTHER	-------------------
			
			/**
			  * @param descriptionLanguageIds Ids of the languages the descriptions are retrieved in
			  *                               (in order from most to least preferred)
			  * @param connection DB Connection (implicit)
			  * @return User language links, including described languages
			  */
			def withDescriptionsInLanguages(descriptionLanguageIds: Seq[Int])(implicit connection: Connection) =
			{
				// Reads languages and familiarities, then attaches descriptions
				val languages = full
				val languageIds = languages.map { _.languageId }.toSet
				val languageDescriptions = DbDescriptions.ofLanguagesWithIds(languageIds).inLanguages(descriptionLanguageIds)
				val familiarityIds = languages.map { _.familiarityId }.toSet
				val familiarityDescriptions = DbDescriptions.ofLanguageFamiliaritiesWithIds(familiarityIds)
					.inLanguages(descriptionLanguageIds)
				languages.map { base =>
					val language = base.language
					val describedLanguage = DescribedLanguage(language,
						languageDescriptions.getOrElse(language.id, Set()).toSet)
					val describedFamiliarity = DescribedLanguageFamiliarity(base.familiarity,
						familiarityDescriptions.getOrElse(base.familiarityId, Set()).toSet)
					DescribedUserLanguage(base, describedLanguage, describedFamiliarity)
				}
			}
			
			/**
			  * Inserts a new user language combination (please make sure to only insert new languages)
			  * @param languageId Id of the known language
			  * @param familiarityId Id of the user's level of familiarity with this language
			  * @param connection DB Connection (implicit)
			  * @return Newly inserted user langauge link
			  */
			def insert(languageId: Int, familiarityId: Int)(implicit connection: Connection) =
				model.insert(UserLanguageData(userId, languageId, familiarityId))
			
			/**
			  * Removes specified languages from the list of known languages
			  * @param languageIds Ids of the languages to remove
			  * @param connection DB Connection (implicit)
			  * @return Number of removed languages
			  */
			def remove(languageIds: Set[Int])(implicit connection: Connection) =
			{
				if (languageIds.nonEmpty)
					connection(Delete(table) + Where(mergeCondition(model.languageIdColumn.in(languageIds)))).updatedRowCount
				else
					0
			}
		}
		
		case class DbUserMembershipId(organizationId: Int) extends UniqueIdAccess[Int]
		{
			// ATTRIBUTES	------------------------
			
			private val factory = MembershipFactory
			
			override val condition = model.withUserId(userId).withOrganizationId(organizationId).toCondition &&
				factory.nonDeprecatedCondition
			
			
			// COMPUTED	----------------------------
			
			private def model = MembershipModel
			
			
			// IMPLEMENTED	------------------------
			
			override def target = factory.target
			
			override def valueToId(value: Value) = value.int
			
			override def table = factory.table
		}
		
		// If email is empty, it is not searched
		class DbUserInvitations(email: Option[String]) extends InvitationsAccess
		{
			override val globalCondition =
			{
				email match
				{
					case Some(email) => Some(model.withRecipientId(userId)
						.withRecipientEmail(email).toConditionWithOperator(combineOperator = Or))
					case None => Some(model.withRecipientId(userId).toCondition)
				}
			}
			
			override protected def defaultOrdering = None
		}
		
		object DbUserMemberships extends ManyModelAccess[Membership]
		{
			// IMPLEMENTED	---------------------------
			
			override def factory = MembershipFactory
			
			override def globalCondition = Some(condition)
			
			override protected def defaultOrdering = None
			
			
			// COMPUTED	--------------------------------
			
			private def model = MembershipModel
			
			private def condition = userCondition && factory.nonDeprecatedCondition
			
			private def userCondition = model.withUserId(userId).toCondition
			
			/**
			  * @param connection DB Connection (implicit)
			  * @return Ids of all the organizations this user is a current member of
			  */
			def organizationIds(implicit connection: Connection) =
				connection(Select(table, model.organizationIdAttName) + Where(condition)).rowIntValues
			
			/**
			  * All organizations & roles associated with these memberships
			  * @param connection DB Connection (implicit)
			  * @return A list of organizations, along with all roles, rights and descriptions that these
			  *         memberships link to
			  */
			def myOrganizations(implicit connection: Connection) =
			{
				// Reads all memberships & roles first
				val memberships = MembershipWithRolesFactory.getMany(userCondition && MembershipWithRolesFactory.nonDeprecatedCondition)
				// Reads organization descriptions
				val organizationIds = memberships.map { _.wrapped.organizationId }.toSet
				if (organizationIds.nonEmpty)
				{
					// TODO: Add proper language filtering
					val organizationDescriptions = DbDescriptions.ofOrganizationsWithIds(organizationIds).all
					// Reads all role right information concerning the targeted roles
					val rolesWithRights = DbUserRoles(memberships.flatMap { _.roleIds }.toSet).withRights
					
					memberships.map { membership =>
						val organizationId = membership.wrapped.organizationId
						MyOrganization(organizationId, userId,
							organizationDescriptions.filter { _.targetId == organizationId }.toSet,
							membership.roleIds.flatMap { roleId => rolesWithRights.find { _.roleId == roleId } })
					}
				}
				else
					Vector()
			}
		}
	}
}
