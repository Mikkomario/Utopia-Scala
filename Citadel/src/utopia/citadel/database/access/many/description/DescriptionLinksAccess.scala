package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.post.NewDescription
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}
import utopia.vault.sql.SqlExtensions._

object DescriptionLinksAccess
{
	// OTHER    -------------------------------
	
	/**
	  * Creates a new description links access point
	  * @param linkTable Description link table (e.g task_description)
	  * @param linkAttName Name of the property that refers to the target table (e.g. taskId)
	  * @return A new access point to links in that table
	  */
	def apply(linkTable: Table, linkAttName: String): DescriptionLinksAccess =
		apply(DescriptionLinkFactory(linkTable, linkAttName))
	
	/**
	  * Creates a new description links access point
	  * @param linkFactory A description link factory
	  * @return An access point to links available through that factory
	  */
	def apply(linkFactory: DescriptionLinkFactory[DescriptionLink]): DescriptionLinksAccess =
		new SimpleDescriptionLinksAccess(linkFactory)
	
	
	// NESTED   -------------------------------
	
	private class SimpleDescriptionLinksAccess(override val factory: DescriptionLinkFactory[DescriptionLink])
		extends DescriptionLinksAccess
}

/**
  * A common trait for access points to description link access points that return multiple links at once
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
trait DescriptionLinksAccess extends DescriptionLinksForManyAccessLike with NonDeprecatedView[DescriptionLink]
{
	// ABSTRACT ----------------------------
	
	override def factory: DescriptionLinkFactory[DescriptionLink]
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def subGroup(remainingTargetIds: Set[Int]) = new DescriptionsOfMany(remainingTargetIds)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param id Id of the targeted item
	  * @return An access point to that item's descriptions
	  */
	def apply(id: Int) = new DescriptionsOfSingle(id)
	/**
	  * @param ids Ids of the targeted items
	  * @return An access point to those items descriptions
	  */
	def apply(ids: Set[Int]) = new DescriptionsOfMany(ids)
	/**
	  * @param ids Ids of the targeted items
	  * @return An access point to those items descriptions
	  */
	def apply(ids: Iterable[Int]) = new DescriptionsOfMany(ids.toSet)
	
	
	// NESTED	----------------------------
	
	/**
	  * Provides access to multiple items' descriptions
	  * @param targetIds Ids of the targeted items
	  */
	class DescriptionsOfMany(targetIds: Set[Int]) extends DescriptionLinksForManyAccessLike with SubView
	{
		// IMPLEMENTED	---------------------
		
		override protected def parent = DescriptionLinksAccess.this
		
		override def filterCondition = linkModel.targetIdColumn.in(targetIds)
		
		override def factory = parent.factory
		
		override protected def subGroup(remainingTargetIds: Set[Int]) =
		{
			if (remainingTargetIds == targetIds)
				this
			else
				new DescriptionsOfMany(remainingTargetIds)
		}
		
		
		// OTHER	-------------------------
		
		/**
		  * Reads descriptions for these items using the specified languages. Only up to one description per
		  * role per target is read.
		  * @param roleIds     Ids of the description roles that are read (will not target roles outside this set)
		  * @param connection  DB Connection (implicit)
		  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
		  *                    language ids are used when no results can be found with the more preferred options)
		  * @return Read descriptions, grouped by target id
		  */
		def withRolesInPreferredLanguages(roleIds: Set[Int])
		                                 (implicit connection: Connection,
		                                  languageIds: LanguageIds): Map[Int, Vector[DescriptionLink]] =
		{
			if (languageIds.isEmpty || targetIds.isEmpty || roleIds.isEmpty)
				Map()
			else
				findInPreferredLanguages(targetIds, roleIds)
		}
		/**
		  * Reads descriptions for these items using the specified languages. Only up to one description per
		  * role per target is read.
		  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
		  *                    language ids are used when no results can be found with the more preferred options)
		  * @param roleIds     Ids of the description roles that are read (will not target roles outside this set)
		  * @param connection  DB Connection (implicit)
		  * @return Read descriptions, grouped by target id
		  */
		@deprecated("Please use withRolesInPreferredLanguages instead", "v1.3")
		def inLanguages(languageIds: Seq[Int], roleIds: Set[Int])
		               (implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
			withRolesInPreferredLanguages(roleIds)(connection, LanguageIds(languageIds.toVector))
		
		/**
		  * Reads descriptions of these items targeting a single description role. If some descriptions couldn't
		  * be found in the first language, the second language is searched as a backup, moving to third, fourth etc.
		  * language when/if necessary.
		  * @param roleId Id of the targeted description role
		  * @param connection Implicit DB Connection
		  * @param languageIds Ids of the languages used in the search, from most preferred to least preferred
		  * @return Description links, each mapped to their target's id
		  */
		def withRoleIdInPreferredLanguages(roleId: Int)
		                                  (implicit connection: Connection,
		                                   languageIds: LanguageIds): Map[Int, DescriptionLink] =
		{
			if (languageIds.isEmpty)
				Map()
			else
				withRoleIdInPreferredLanguages(roleId, targetIds)
		}
		/**
		  * Reads descriptions of these items targeting a single description role. If some descriptions couldn't
		  * be found in the first language, the second language is searched as a backup, moving to third, fourth etc.
		  * language when/if necessary.
		  * @param roleId Id of the targeted description role
		  * @param languageIds Ids of the languages used in the search, from most preferred to least preferred
		  * @param connection Implicit DB Connection
		  * @return Description links, each mapped to their target's id
		  */
		@deprecated("Please use withRoleIdInPreferredLanguages instead", "v1.3")
		def withRoleIdInLanguages(roleId: Int, languageIds: Seq[Int])
		                         (implicit connection: Connection): Map[Int, DescriptionLink] =
			withRoleIdInPreferredLanguages(roleId)(connection, LanguageIds(languageIds.toVector))
		/**
		 * Reads descriptions of these items targeting a single description role. If some descriptions couldn't
		 * be found in the first language, the second language is searched as a backup, moving to third, fourth etc.
		 * language when/if necessary.
		 * @param roleId Id of the targeted description role
		 * @param languageIds Ids of the languages used in the search, from most preferred to least preferred
		 * @param connection Implicit DB Connection
		 * @return Description links, each mapped to their target's id
		 */
		@deprecated("Please use withRoleIdInPreferredLanguages instead", "v1.3")
		def forRoleInLanguages(roleId: Int, languageIds: Seq[Int])
		                      (implicit connection: Connection): Map[Int, DescriptionLink] =
			withRoleIdInPreferredLanguages(roleId)(connection, LanguageIds(languageIds.toVector))
	}
	
	/**
	  * Provides access to all descriptions of a single item
	  * @param targetId Id of the target item
	  */
	class DescriptionsOfSingle(targetId: Int) extends DescriptionLinksAccessLike with SubView
	{
		// COMPUTED ------------------------
		
		/**
		  * @param connection  DB Connection (implicit)
		  * @param languageIds Ids of the targeted languages (in order from most to least preferred)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def inPreferredLanguages(implicit connection: Connection, languageIds: LanguageIds): Vector[DescriptionLink] =
		{
			languageIds.headOption match
			{
				case Some(languageId) =>
					val allRoleIds = DbDescriptionRoleIds.all.toSet
					val readDescriptions = inLanguageWithId(languageId).all
					val missingRoleIds = allRoleIds -- readDescriptions.map { _.description.roleId }.toSet
					if (missingRoleIds.nonEmpty && languageIds.size > 1)
						readDescriptions ++ withRolesInPreferredLanguages(missingRoleIds)(connection, languageIds.tail)
					else
						readDescriptions
				case None => Vector()
			}
		}
		
		
		// IMPLEMENTED	--------------------
		
		override protected def parent = DescriptionLinksAccess.this
		
		override def filterCondition = linkModel.withTargetId(targetId).toCondition
		
		override def factory = parent.factory
		
		
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
			linkModel.insert(targetId, newDescription)
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
			linkModel.nowDeprecated.updateWhere(mergeCondition(descriptionCondition), Some(factory.target)) > 0
		}
		
		/**
		  * @param languageIds Ids of the targeted languages (in order from most to least preferred)
		  * @param connection  DB Connection (implicit)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		@deprecated("Please use inPreferredLanguages instead", "v1.3")
		def inLanguages(languageIds: Seq[Int])(implicit connection: Connection): Vector[DescriptionLink] =
			inPreferredLanguages(connection, LanguageIds(languageIds.toVector))
		
		/**
		  * @param roleIds Ids of the roles that need descriptions
		  * @param connection       DB Connection (implicit)
		  * @param languageIds      Ids of the targeted languages (in order from most to least preferred)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def withRolesInPreferredLanguages(roleIds: Set[Int])(
			implicit connection: Connection, languageIds: LanguageIds): Vector[DescriptionLink] =
		{
			// Reads descriptions in target languages until either all description types have been read or all language
			// options exhausted
			languageIds.headOption match {
				case Some(languageId) =>
					val readDescriptions = inLanguageWithId(languageId).withRoleIds(roleIds)
					val newRemainingRoleIds = roleIds -- readDescriptions.map { _.description.roleId }
					if (newRemainingRoleIds.nonEmpty && languageIds.size > 1)
						readDescriptions ++ withRolesInPreferredLanguages(newRemainingRoleIds)(
							connection, languageIds.tail)
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
		@deprecated("Please use withRolesInPreferredLanguages instead", "v1.3")
		def inLanguages(languageIds: Seq[Int], remainingRoleIds: Set[Int])
		               (implicit connection: Connection): Vector[DescriptionLink] =
			withRolesInPreferredLanguages(remainingRoleIds)(connection, LanguageIds(languageIds.toVector))
	}
}
