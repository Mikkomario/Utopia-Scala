package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.post.NewDescription
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing various types of descriptions
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1.0
  */
object DbDescriptions
{
	// COMPUTED	----------------------------
	
	/**
	  * An access point to all description role descriptions
	  */
	val ofAllDescriptionRoles = DescriptionsOfAll(DescriptionLinkFactory.descriptionRole,
		DescriptionLinkModel.descriptionRole)
	/**
	  * An access point to all language descriptions
	  */
	val ofAllLanguages = DescriptionsOfAll(DescriptionLinkFactory.language,
		DescriptionLinkModel.language)
	/**
	  * An access point to all language familiarity description
	  */
	val ofAllLanguageFamiliarities = DescriptionsOfAll(DescriptionLinkFactory.languageFamiliarity,
		DescriptionLinkModel.languageFamiliarity)
	/**
	  * An access point to all role descriptions
	  */
	val ofAllUserRoles = DescriptionsOfAll(DescriptionLinkFactory.userRole, DescriptionLinkModel.userRole)
	/**
	  * An access point to all task descriptions
	  */
	val ofAllTasks = DescriptionsOfAll(DescriptionLinkFactory.task, DescriptionLinkModel.task)
	/**
	  * An access point to all organization descriptions
	  */
	val ofAllOrganizations = DescriptionsOfAll(DescriptionLinkFactory.organization, DescriptionLinkModel.organization)
	/**
	  * An access point to all device descriptions
	  */
	val ofAllDevices = DescriptionsOfAll(DescriptionLinkFactory.device, DescriptionLinkModel.device)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param organizationId Organization id
	  * @return An access point to that organization's descriptions
	  */
	def ofOrganizationWithId(organizationId: Int) =
		DescriptionsOfSingle(organizationId, DescriptionLinkFactory.organization, DescriptionLinkModel.organization)
	/**
	  * @param organizationIds Organization ids
	  * @return An access point to descriptions of those organizations
	  */
	def ofOrganizationsWithIds(organizationIds: Set[Int]) = DescriptionsOfMany(organizationIds,
		DescriptionLinkFactory.organization, DescriptionLinkModel.organization)
	
	/**
	  * @param deviceId Device id
	  * @return An access point to that device's descriptions
	  */
	def ofDeviceWithId(deviceId: Int) =
		DescriptionsOfSingle(deviceId, DescriptionLinkFactory.device, DescriptionLinkModel.device)
	/**
	  * @param deviceIds Device ids
	  * @return An access point to descriptions of those devices
	  */
	def ofDevicesWithIds(deviceIds: Set[Int]) = DescriptionsOfMany(deviceIds,
		DescriptionLinkFactory.device, DescriptionLinkModel.device)
	
	/**
	  * @param taskId Task id
	  * @return An access point to descriptions of that task type
	  */
	def ofTaskWithId(taskId: Int) = DescriptionsOfSingle(taskId, DescriptionLinkFactory.task,
		DescriptionLinkModel.task)
	/**
	  * @param taskIds Ids of targeted tasks
	  * @return An access point to descriptions of those task types
	  */
	def ofTasksWithIds(taskIds: Set[Int]) = DescriptionsOfMany(taskIds,
		DescriptionLinkFactory.task, DescriptionLinkModel.task)
	
	/**
	  * @param roleId User role id
	  * @return An access point to descriptions of that user role
	  */
	def ofUserRoleWithId(roleId: Int) =
		DescriptionsOfSingle(roleId, DescriptionLinkFactory.userRole, DescriptionLinkModel.userRole)
	/**
	  * @param roleIds Ids of targeted user roles
	  * @return An access point to descriptions of those roles
	  */
	def ofUserRolesWithIds(roleIds: Set[Int]) = DescriptionsOfMany(roleIds,
		DescriptionLinkFactory.userRole, DescriptionLinkModel.userRole)
	
	/**
	  * @param languageId Language id
	  * @return An access point to that language's descriptions
	  */
	def ofLanguageWithId(languageId: Int) =
		DescriptionsOfSingle(languageId, DescriptionLinkFactory.language, DescriptionLinkModel.language)
	/**
	  * @param languageIds Language ids
	  * @return An access point to descriptions of languages with those ids
	  */
	def ofLanguagesWithIds(languageIds: Set[Int]) = DescriptionsOfMany(languageIds,
		DescriptionLinkFactory.language, DescriptionLinkModel.language)
	
	// TODO: WET WET (factory and model references are being repeated 3 times each)
	
	/**
	  * @param familiarityId Language familiarity id
	  * @return An access point to that familiarity's descriptions
	  */
	def ofLanguageFamiliarityWithId(familiarityId: Int) = DescriptionsOfSingle(familiarityId,
		DescriptionLinkFactory.languageFamiliarity, DescriptionLinkModel.languageFamiliarity)
	/**
	  * @param familiarityIds A set of language familiarity ids
	  * @return An access point to those familiarities descriptions
	  */
	def ofLanguageFamiliaritiesWithIds(familiarityIds: Set[Int]) = DescriptionsOfMany(familiarityIds,
		DescriptionLinkFactory.languageFamiliarity, DescriptionLinkModel.languageFamiliarity)
	
	/**
	  * Creates a new access point to a custom description table + link column combination
	  * @param linkTable Table that contains targeted description links
	  * @param linkAttName Name of the property that contains links to the described items
	  * @return A new descriptions access point
	  */
	def access(linkTable: Table, linkAttName: String) =
	{
		val factory = DescriptionLinkFactory(linkTable, linkAttName)
		DescriptionsOfAll(factory, factory.modelFactory)
	}
	
	
	// NESTED	----------------------------
	
	case class DescriptionsOfAll(factory: DescriptionLinkFactory[DescriptionLink],
	                             linkModelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinksForManyAccess
	{
		// IMPLEMENTED  --------------------
		
		override def globalCondition = Some(factory.nonDeprecatedCondition)
		
		override protected def subGroup(remainingTargetIds: Set[Int]) =
			DescriptionsOfMany(remainingTargetIds, factory, linkModelFactory)
		
		
		// OTHER    ------------------------
		
		/**
		  * @param targetId Id of the targeted / described item
		  * @return An access point to that item's descriptions
		  */
		def apply(targetId: Int) = DescriptionsOfSingle(targetId, factory, linkModelFactory)
		
		/**
		  * @param ids A set of description target item ids
		  * @return An access point to descriptions of those items
		  */
		def apply(ids: Set[Int]) = DescriptionsOfMany(ids, factory, linkModelFactory)
	}
	
	/**
	  * Provides access to multiple items' descriptions
	  * @param targetIds Ids of the targeted items
	  * @param factory A description link factory
	  * @param linkModelFactory A description link model factory
	  */
	case class DescriptionsOfMany(targetIds: Set[Int], factory: DescriptionLinkFactory[DescriptionLink],
	                              linkModelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinksForManyAccess
	{
		// IMPLEMENTED	---------------------
		
		// Alters the condition based on the number of targeted items
		override val globalCondition = {
			if (targetIds.isEmpty)
				Some(Condition.alwaysFalse)
			else {
				val baseCondition = {
					if (targetIds.size == 1)
						linkModelFactory.withTargetId(targetIds.head).toCondition
					else
						linkModelFactory.targetIdColumn.in(targetIds)
				}
				Some(baseCondition && factory.nonDeprecatedCondition)
			}
		}
		
		override protected def subGroup(remainingTargetIds: Set[Int]) =
		{
			if (remainingTargetIds == targetIds)
				this
			else
				copy(targetIds = remainingTargetIds)
		}
		
		
		// OTHER	-------------------------
		
		/**
		  * Reads descriptions for these items using the specified languages. Only up to one description per
		  * role per target is read.
		  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
		  *                    language ids are used when no results can be found with the more preferred options)
		  * @param roleIds     Ids of the description roles that are read (will not target roles outside this set)
		  * @param connection  DB Connection (implicit)
		  * @return Read descriptions, grouped by target id
		  */
		def inLanguages(languageIds: Seq[Int], roleIds: Set[Int])
		               (implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
		{
			if (languageIds.isEmpty || targetIds.isEmpty || roleIds.isEmpty)
				Map()
			else
				inLanguages(targetIds, languageIds, roleIds)
		}
	}
	
	/**
	  * Provides access to all descriptions of a single item
	  * @param targetId Id of the target item
	  * @param factory A link factory instance
	  * @param linkModelFactory A link model factory instance
	  */
	case class DescriptionsOfSingle(targetId: Int, factory: DescriptionLinkFactory[DescriptionLink],
	                                linkModelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override val globalCondition = Some(linkModelFactory.withTargetId(targetId).toCondition &&
			factory.nonDeprecatedCondition)
		
		
		// OTHER	-------------------------
		
		/**
		  * Updates possibly multiple descriptions for this item (will replace old description versions)
		  * @param newDescription New description to insert
		  * @param authorId       Id of the user who wrote this description
		  * @param connection     DB Connection (implicit)
		  * @return Newly inserted description links
		  */
		def update(newDescription: NewDescription, authorId: Int)(implicit connection: Connection): Vector[DescriptionLink] =
		{
			// Updates each role + text pair separately
			newDescription.descriptions.map { case (role, text) =>
				update(DescriptionData(role, newDescription.languageId, text, Some(authorId)))
			}.toVector
		}
		
		/**
		  * Updates a single description for this item
		  * @param newDescription New description
		  * @param connection     DB Connection
		  * @return Newly inserted description
		  */
		def update(newDescription: DescriptionData)(implicit connection: Connection) =
		{
			// Must first deprecate the old version of this description
			deprecate(newDescription.languageId, newDescription.roleId)
			// Then inserts a new description
			linkModelFactory.insert(targetId, newDescription)
		}
		
		/**
		  * Updates a single description for this item
		  * @param newDescriptionRoleId Id of the new description's role
		  * @param languageId           Id of the language the new description is written in
		  * @param authorId             Id of the user who wrote this description
		  * @param text                 Description text
		  * @param connection           DB Connection (implicit)
		  * @return Newly inserted description
		  */
		def update(newDescriptionRoleId: Int, languageId: Int, authorId: Int, text: String)
		          (implicit connection: Connection): DescriptionLink = update(DescriptionData(newDescriptionRoleId,
			languageId, text, Some(authorId)))
		
		/**
		  * Deprecates a description for this item
		  * @param languageId Id of the language the description is written in
		  * @param roleId     Id of the targeted description role
		  * @param connection DB Connection (implicit)
		  * @return Whether a description was deprecated
		  */
		def deprecate(languageId: Int, roleId: Int)(implicit connection: Connection) =
		{
			// Needs to join into description table in order to filter by language id and role id
			// (factories automatically do this)
			val descriptionCondition = descriptionModel.withRoleId(roleId).withLanguageId(languageId).toCondition
			linkModelFactory.nowDeprecated.updateWhere(mergeCondition(descriptionCondition), Some(factory.target)) > 0
		}
		
		/**
		  * @param languageIds Ids of the targeted languages (in order from most to least preferred)
		  * @param connection  DB Connection (implicit)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def inLanguages(languageIds: Seq[Int])(implicit connection: Connection): Vector[DescriptionLink] =
		{
			languageIds.headOption match {
				case Some(languageId) =>
					val allRoleIds = DbDescriptionRoleIds.all.toSet
					val readDescriptions = inLanguageWithId(languageId).all
					val missingRoleIds = allRoleIds -- readDescriptions.map { _.description.roleId }.toSet
					if (missingRoleIds.nonEmpty)
						readDescriptions ++ inLanguages(languageIds.tail, missingRoleIds)
					else
						readDescriptions
				case None => Vector()
			}
		}
		
		/**
		  * @param languageIds      Ids of the targeted languages (in order from most to least preferred)
		  * @param remainingRoleIds Ids of the roles that need descriptions
		  * @param connection       DB Connection (implicit)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def inLanguages(languageIds: Seq[Int], remainingRoleIds: Set[Int])(
			implicit connection: Connection): Vector[DescriptionLink] =
		{
			// Reads descriptions in target languages until either all description types have been read or all language
			// options exhausted
			languageIds.headOption match {
				case Some(languageId) =>
					val readDescriptions = inLanguageWithId(languageId).forRolesWithIds(remainingRoleIds)
					val newRemainingRoleIds = remainingRoleIds -- readDescriptions.map { _.description.roleId }
					if (remainingRoleIds.nonEmpty)
						readDescriptions ++ inLanguages(languageIds.tail, newRemainingRoleIds)
					else
						readDescriptions
				case None => Vector()
			}
		}
	}
}
