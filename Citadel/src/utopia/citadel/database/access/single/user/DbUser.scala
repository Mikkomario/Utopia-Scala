package utopia.citadel.database.access.single.user

import utopia.citadel.database.access.id.many.DbUserIds
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
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.access.single.column.UniqueIdAccess
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.single.model.distinct.{SingleIntIdModelAccess, UniqueModelAccess}
import utopia.vault.sql.{Delete, Exists, Select, Where}
import utopia.vault.sql.SqlExtensions._

import java.time.Instant
import scala.util.{Failure, Success}

/**
  * Used for accessing individual user's data
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbUser extends SingleModelAccess[User]
{
	// COMPUTED -------------------------
	
	// private def languageLinkModel = UserLanguageModel
	private def membershipModel = MembershipModel
	
	
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
	
	case class DbSingleUser(override val id: Int) extends SingleIntIdModelAccess[User]
	{
		// COMPUTED	---------------------
		
		/**
		 * @return Id of the targeted user
		 */
		def userId = id
		
		/**
		  * @return An access point to this user's known languages
		  */
		def languages = DbSingleUserLanguages
		/**
		  * @param connection DB Connection
		  * @return Ids of the languages known by this user
		  */
		def languageIds(implicit connection: Connection) = connection(Select(UserLanguageModel.table,
			UserLanguageModel.languageIdAttName) + Where(UserLanguageModel.withUserId(id).toCondition)).rowIntValues
		/*
		def primaryLanguageId(implicit connection: Connection) =
		{
			// TODO: For this we need a languageFamiliarity model, which is missing
			// val familiarityModel = LanguageFamiliarityMo
			connection(Select(languageLinkModel.table join ))
		}*/
		
		/**
		  * @param connection DB Connection
		  * @return Ids of the devices this user has used
		  */
		def deviceIds(implicit connection: Connection) = connection(Select(UserDeviceModel.table,
			UserDeviceModel.deviceIdAttName) + Where(UserDeviceModel.withUserId(id).toCondition &&
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
		def settings = DbSingleUserSettings
		
		/**
		  * @param connection DB Connection (implicit), used for reading user email address
		  * @return An access point to invitations for this user
		  */
		// Will need to read settings for accessing since joining logic would get rather complex otherwise
		// TODO: May need to use a different search logic for users that don't have an email address
		def receivedInvitations(implicit connection: Connection) = new DbSingleUserInvitations(settings.flatMap { _.email })
		
		/**
		  * @return An access point to this user's memberships
		  */
		def memberships = DbSingleUserMemberships
		
		
		// IMPLEMENTED  ------------------
		
		override def factory = DbUser.factory
		
		
		// OTHER	----------------------
		
		/**
		  * @param organizationId Id of the targeted organization
		  * @param connection     DB Connection (implicit)
		  * @return Whether this user is a member of the specified organization
		  */
		def isMemberInOrganizationWithId(organizationId: Int)(implicit connection: Connection) =
			membershipIdInOrganizationWithId(organizationId).isDefined
		
		/**
		  * @param organizationIds A set of organization ids
		  * @param connection      Implicit DB Connection
		  * @return Whether this user is a member of any of those organizations
		  */
		def isMemberOfAnyOrganizationOfIds(organizationIds: Iterable[Int])(implicit connection: Connection) =
			memberships.exists(membershipModel.organizationIdColumn.in(organizationIds))
		
		/**
		  * @param organizationId Id of targeted organization
		  * @return An access point to this user's membership id in that organization
		  */
		def membershipIdInOrganizationWithId(organizationId: Int) =
			DbSingleUserMembershipId(organizationId)
		
		/**
		  * Checks whether this user is a member of one or more organizations the other user is a member of
		  * @param otherUserId Another user's id
		  * @param connection  Implicit DB Connection
		  * @return Whether these two users have at least one common organization
		  */
		def sharesOrganizationWithUserWithId(otherUserId: Int)(implicit connection: Connection) =
		{
			// Case: Testing against self
			if (id == otherUserId)
				true
			// Case: Testing against other user
			else {
				// First reads the organization ids of this user and then checks whether the other user
				// is a member in any of them
				// This is to avoid joining same table (membership) twice
				val myOrganizationIds = memberships.organizationIds
				myOrganizationIds.nonEmpty && apply(otherUserId).isMemberOfAnyOrganizationOfIds(myOrganizationIds)
			}
		}
		
		/**
		  * Links this user with the specified device
		  * @param deviceId   Id of targeted device (must be valid)
		  * @param connection DB Connection (implicit)
		  * @return Whether a new link was created (false if there already existed a link between this user and the device)
		  */
		def linkWithDeviceWithId(deviceId: Int)(implicit connection: Connection) =
		{
			// Checks whether there already exists a connection between this user and specified device
			if (UserDeviceModel.exists(UserDeviceModel.withUserId(id).withDeviceId(deviceId).toCondition &&
				UserDeviceModel.nonDeprecatedCondition))
				false
			else {
				UserDeviceModel.insert(id, deviceId)
				true
			}
		}
		
		
		// NESTED	-----------------------
		
		object DbSingleUserSettings extends UniqueModelAccess[UserSettings]
		{
			// IMPLEMENTED	---------------
			
			override def condition = model.withUserId(userId).toCondition && factory.nonDeprecatedCondition
			
			override def factory = UserSettingsFactory
			
			
			// COMPUTED	-------------------
			
			private def model = UserSettingsModel
			
			/**
			  * @param connection Implicit DB Connection
			  * @return This user's current user name
			  */
			def name(implicit connection: Connection) = pullAttribute(model.userNameAttName).string
			
			/**
			  * @param connection Implicit DB Connection
			  * @return This user's current email address
			  */
			def email(implicit connection: Connection) = pullAttribute(model.emailAttName).string
			
			
			// OTHER	-------------------
			
			/**
			  * Updates this user's current settings
			  * @param newSettings           New user settings version
			  * @param requireUniqueUserName Whether user names should be required to be unique under all circumstances
			  * @param connection            DB Connection (implicit)
			  * @return Newly inserted settings. Failure if the email address is reserved for another user
			  *         (or if user name is taken in case an email address was not provided).
			  */
			def update(newSettings: UserSettingsData, requireUniqueUserName: Boolean = false)
			          (implicit connection: Connection) =
			{
				def _replace() =
				{
					// Deprecates the old settings
					model.nowDeprecated.updateWhere(condition)
					// Inserts new settings
					Success(model.insert(userId, newSettings))
				}
				
				def _userNameIsValid() = DbUserIds.forName(newSettings.name).forall { _ == userId }
				
				newSettings.email match {
					// Case: User has specified an email address => it will have to be unique
					case Some(email) =>
						// Makes sure the email address is still available (or belongs to this user)
						// May also check the user name
						if (DbUserId.forEmail(email).forall { _ == userId } &&
							(!requireUniqueUserName || _userNameIsValid()))
							_replace()
						else
							Failure(new AlreadyUsedException(s"Email address $email is already in use by another user"))
					// Case: User hasn't specified an email address => the user name must be unique
					case None =>
						if (_userNameIsValid())
							_replace()
						else
							Failure(new AlreadyUsedException(
								s"User name ${ newSettings.name } is already in use by another user"))
				}
			}
		}
		
		// TODO: This should be moved under DbUserLanguages (same with the other non-user access points)
		object DbSingleUserLanguages extends ManyModelAccess[UserLanguage]
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
			  * @param connection             DB Connection (implicit)
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
			  * @param languageId    Id of the known language
			  * @param familiarityId Id of the user's level of familiarity with this language
			  * @param connection    DB Connection (implicit)
			  * @return Newly inserted user langauge link
			  */
			def insert(languageId: Int, familiarityId: Int)(implicit connection: Connection) =
				model.insert(UserLanguageData(userId, languageId, familiarityId))
			
			/**
			  * Removes specified languages from the list of known languages
			  * @param languageIds Ids of the languages to remove
			  * @param connection  DB Connection (implicit)
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
		
		case class DbSingleUserMembershipId(organizationId: Int) extends UniqueIdAccess[Int]
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
		class DbSingleUserInvitations(email: Option[String]) extends InvitationsAccess
		{
			override val globalCondition = {
				email match {
					case Some(email) => Some(model.withRecipientId(userId)
						.withRecipientEmail(email).toConditionWithOperator(combineOperator = Or))
					case None => Some(model.withRecipientId(userId).toCondition)
				}
			}
			
			override protected def defaultOrdering = None
		}
		
		object DbSingleUserMemberships extends ManyModelAccess[Membership]
		{
			// COMPUTED	--------------------------------
			
			private def model = membershipModel
			
			private def userCondition = model.withUserId(userId).toCondition
			
			private def condition = userCondition && factory.nonDeprecatedCondition
			
			/**
			  * @param connection DB Connection (implicit)
			  * @return Ids of all the organizations this user is a current member of
			  */
			def organizationIds(implicit connection: Connection) =
				connection(Select(table, model.organizationIdAttName) + Where(condition)).rowIntValues
			
			
			// IMPLEMENTED	---------------------------
			
			override def factory = MembershipFactory
			
			override def globalCondition = Some(condition)
			
			override protected def defaultOrdering = None
			
			
			// OTHER    ------------------------------
			
			/**
			  * Reads described organization and role information for this user
			  * @param languageIds Ids of the languages in which descriptions are retrieved
			  *                    (from most preferred to least preferred)
			  * @param connection  Implicit DB Connection
			  * @return This user's organizations and roles within those organizations
			  */
			def myOrganizations(languageIds: Seq[Int])(implicit connection: Connection): Vector[MyOrganization] =
				myOrganizations(languageIds, None).get
			
			/**
			  * Reads described organization and role information for this user
			  * @param languageIds         Ids of the languages in which descriptions are retrieved
			  *                            (from most preferred to least preferred)
			  * @param ifModifiedThreshold A time threshold for checking if the results have been modified
			  *                            since that time
			  * @param connection          Implicit DB Connection
			  * @return This user's organizations and roles within those organizations.
			  *         None if the items were not modified since the threshold time.
			  */
			def myOrganizations(languageIds: Seq[Int], ifModifiedThreshold: Instant)
			                   (implicit connection: Connection): Option[Vector[MyOrganization]] =
				myOrganizations(languageIds, Some(ifModifiedThreshold))
			
			/**
			  * Reads described organization and role information for this user
			  * @param languageIds         Ids of the languages in which descriptions are retrieved
			  *                            (from most preferred to least preferred)
			  * @param ifModifiedThreshold An optional time threshold for checking if the results have been modified
			  *                            since that time (default = None)
			  * @param connection          Implicit DB Connection
			  * @return This user's organizations and roles within those organizations.
			  *         None if 'ifModifiedSinceThreshold' -parameter was specified and the items were not modified
			  *         since that time.
			  */
			def myOrganizations(languageIds: Seq[Int], ifModifiedThreshold: Option[Instant])
			                   (implicit connection: Connection) =
			{
				// Reads all memberships & roles first
				val memberships = MembershipWithRolesFactory
					.getMany(userCondition && MembershipWithRolesFactory.nonDeprecatedCondition)
				// Reads organization descriptions
				val organizationIds = memberships.map { _.wrapped.organizationId }.toSet
				// Case: Organizations / memberships found => Reads descriptions (may check for modified state first)
				if (organizationIds.nonEmpty) {
					// Case: Modified or modification not checked
					if (ifModifiedThreshold.forall { t =>
						wereModifiedSince(t) ||
							DbDescriptions.ofOrganizationsWithIds(organizationIds).isModifiedSince(t)
					}) {
						// Reads organization descriptions
						val organizationDescriptions = DbDescriptions.ofOrganizationsWithIds(organizationIds)
							.inLanguages(languageIds)
						// Reads all role right information concerning the targeted roles
						val rolesWithRights = DbUserRoles(memberships.flatMap { _.roleIds }.toSet).withRights
						
						Some(memberships.map { membership =>
							val organizationId = membership.wrapped.organizationId
							MyOrganization(organizationId, userId,
								organizationDescriptions.getOrElse(organizationId, Vector()).toSet,
								membership.roleIds.flatMap { roleId => rolesWithRights.find { _.roleId == roleId } })
						})
					}
					// Case: Not modified
					else
						None
				}
				// Case: No organizations / memberships found => Returns an empty vector (or None if not modified)
				else {
					if (ifModifiedThreshold.forall(wereModifiedSince))
						Some(Vector())
					else
						None
				}
			}
			
			/**
			  * Checks whether this user's memberships were modified after the specified time threshold
			  * @param threshold  A time threshold
			  * @param connection Implicit DB Connection
			  * @return Whether these memberships were modified after that threshold
			  */
			def wereModifiedSince(threshold: Instant)(implicit connection: Connection) =
				Exists(target, userCondition &&
					(factory.createdAfterCondition(threshold) || model.deprecatedAfterCondition(threshold)))
		}
	}
}
