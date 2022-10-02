package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.citadel.database.factory.description.LinkedDescriptionFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.post.NewDescription
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}
import utopia.vault.sql.SqlExtensions._

object LinkedDescriptionsAccess
{
	// OTHER    -----------------------------
	
	/**
	  * @param factory Factory used for reading linked descriptions
	  * @param linkModel Factory used for creating description link database models
	  * @return An access point to those descriptions with links
	  */
	def apply(factory: LinkedDescriptionFactory, linkModel: DescriptionLinkModelFactory): LinkedDescriptionsAccess =
		new SimpleLinkedDescriptionsAccess(factory, linkModel)
	/**
	  * @param table A description link table
	  * @return An access point to descriptions linked with that table
	  */
	def apply(table: DescriptionLinkTable): LinkedDescriptionsAccess =
		apply(LinkedDescriptionFactory(table), DescriptionLinkModelFactory(table))
	
	
	// NESTED   -----------------------------
	
	private class SimpleLinkedDescriptionsAccess(override val factory: LinkedDescriptionFactory,
	                                             override val linkModel: DescriptionLinkModelFactory)
		extends LinkedDescriptionsAccess
}

/**
  * A common trait for root access points to linked descriptions that return multiple links at once
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
trait LinkedDescriptionsAccess extends LinkedDescriptionsForManyAccessLike with NonDeprecatedView[LinkedDescription]
{
	// ABSTRACT ----------------------------
	
	override def factory: LinkedDescriptionFactory
	
	
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
	class DescriptionsOfMany(targetIds: Set[Int]) extends LinkedDescriptionsForManyAccessLike with SubView
	{
		// IMPLEMENTED	---------------------
		
		/**
		 * Reads descriptions for these items using the specified languages. Only up to one description per
		 * role per target is read.
		 * @param connection  DB Connection (implicit)
		 * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
		 *                    language ids are used when no results can be found with the more preferred options)
		 * @return Read descriptions, grouped by target id
		 */
		def inPreferredLanguages(implicit connection: Connection,
		                                  languageIds: LanguageIds): Map[Int, Vector[LinkedDescription]] =
			languageIds.headOption match
			{
				case Some(languageId: Int) =>
					// In the first iteration, reads all descriptions. After that divides into sub-groups
					val readDescriptions = inLanguageWithId(languageId).pull.groupBy { _.targetId }
					// Reads the rest of the data using recursion
					if (languageIds.size > 1)
						readRemaining(DbDescriptionRoleIds.all.toSet, this, readDescriptions)
					else
						readDescriptions
				case None => Map()
			}
		
		override protected def parent = LinkedDescriptionsAccess.this
		
		override def filterCondition = linkModel.targetIdColumn.in(targetIds)
		
		override def factory = parent.factory
		
		override def linkModel = parent.linkModel
		
		override protected def subGroup(remainingTargetIds: Set[Int]) =
		{
			if (remainingTargetIds == targetIds)
				this
			else
				new DescriptionsOfMany(targetIds & remainingTargetIds)
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
		                                  languageIds: LanguageIds): Map[Int, Vector[LinkedDescription]] =
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
		               (implicit connection: Connection): Map[Int, Vector[LinkedDescription]] =
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
		                                   languageIds: LanguageIds): Map[Int, LinkedDescription] =
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
		                         (implicit connection: Connection): Map[Int, LinkedDescription] =
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
		                      (implicit connection: Connection): Map[Int, LinkedDescription] =
			withRoleIdInPreferredLanguages(roleId)(connection, LanguageIds(languageIds.toVector))
	}
	
	/**
	  * Provides access to all descriptions of a single item
	  * @param targetId Id of the target item
	  */
	class DescriptionsOfSingle(targetId: Int) extends LinkedDescriptionsAccessLike with SubView
	{
		// COMPUTED ------------------------
		
		/**
		  * @param connection  DB Connection (implicit)
		  * @param languageIds Ids of the targeted languages (in order from most to least preferred)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def inPreferredLanguages(implicit connection: Connection, languageIds: LanguageIds): Vector[LinkedDescription] =
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
		
		override protected def parent = LinkedDescriptionsAccess.this
		
		override def filterCondition = linkModel.withTargetId(targetId).toCondition
		
		override def factory = parent.factory
		
		override def linkModel = parent.linkModel
		
		
		// OTHER	-------------------------
		
		/**
		  * Updates possibly multiple descriptions for this item (will replace old description versions)
		  * @param newDescription New description to insert
		  * @param authorId       Id of the user who wrote this description (if known)
		  * @param connection     DB Connection (implicit)
		  * @return Newly inserted description links
		  */
		def update(newDescription: NewDescription, authorId: Option[Int] = None)
		          (implicit connection: Connection): Vector[LinkedDescription] =
		{
			// Updates each role + text pair separately
			newDescription.descriptions.map { case (role, text) =>
				update(DescriptionData(role, newDescription.languageId, text, authorId))
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
		          (implicit connection: Connection): LinkedDescription = update(DescriptionData(newDescriptionRoleId,
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
			descriptionModel.nowDeprecated.updateWhere(mergeCondition(descriptionCondition), Some(factory.target)) > 0
		}
		
		/**
		  * @param languageIds Ids of the targeted languages (in order from most to least preferred)
		  * @param connection  DB Connection (implicit)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		@deprecated("Please use inPreferredLanguages instead", "v1.3")
		def inLanguages(languageIds: Seq[Int])(implicit connection: Connection): Vector[LinkedDescription] =
			inPreferredLanguages(connection, LanguageIds(languageIds.toVector))
		
		/**
		  * @param roleIds Ids of the roles that need descriptions
		  * @param connection       DB Connection (implicit)
		  * @param languageIds      Ids of the targeted languages (in order from most to least preferred)
		  * @return This item's descriptions in specified languages (secondary languages are used when no primary
		  *         language description is found)
		  */
		def withRolesInPreferredLanguages(roleIds: Set[Int])(
			implicit connection: Connection, languageIds: LanguageIds): Vector[LinkedDescription] =
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
		               (implicit connection: Connection): Vector[LinkedDescription] =
			withRolesInPreferredLanguages(remainingRoleIds)(connection, LanguageIds(languageIds.toVector))
	}
}
