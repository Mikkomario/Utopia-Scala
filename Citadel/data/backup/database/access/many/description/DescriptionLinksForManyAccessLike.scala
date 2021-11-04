package utopia.citadel.database.access.many.description

import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.stored.description.DescriptionLinkOld
import utopia.vault.database.Connection

/**
  * A common trait for access points which provide description link access for multiple items at once
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1.0
  */
trait DescriptionLinksForManyAccessLike extends DescriptionLinksAccessLike
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @param remainingTargetIds Target item ids
	  * @return An access point that targets only those items
	  */
	protected def subGroup(remainingTargetIds: Set[Int]): DescriptionLinksForManyAccessLike
	
	/*
	  * Reads descriptions for these items using the specified languages. Only up to one description per
	  * role per target is read.
	  * @param connection  DB Connection (implicit)
	  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
	  *                    language ids are used when no results can be found with the more preferred options)
	  * @return Read descriptions, grouped by target id
	  */
	// def inPreferredLanguages(implicit connection: Connection, languageIds: LanguageIds): Map[Int, Vector[DescriptionLink]]
		/*
		languageIds.headOption match
		{
			case Some(languageId: Int) =>
				// In the first iteration, reads all descriptions. After that divides into sub-groups
				val readDescriptions = inLanguageWithId(languageId).all.groupBy { _.targetId }
				// Reads the rest of the data using recursion
				readRemaining(DbDescriptionRoleIds.all.toSet, this, readDescriptions)
			case None => Map()
		}*/
	
	
	// OTHER	------------------------------------
	
	/*
	  * Reads descriptions for these items using the specified languages. Only up to one description per
	  * role per target is read.
	  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
	  *                    language ids are used when no results can be found with the more preferred options)
	  * @param connection  DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
		/*
	@deprecated("Please use inPreferredLanguages instead", "v1.3")
	def inLanguages(languageIds: Seq[Int])(implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
		inPreferredLanguages(connection, LanguageIds(languageIds.toVector))
	*/
	/**
	  * Reads description data from specified targets
	  * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	  * @param remainingRoleIds   Ids of the remaining description roles to read (shouldn't be empty)
	  * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	  * @param connection         DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
	protected def findInPreferredLanguages(remainingTargetIds: Set[Int], remainingRoleIds: Set[Int])(
		implicit connection: Connection, languageIds: LanguageIds): Map[Int, Vector[DescriptionLinkOld]] =
	{
		// Reads descriptions in target languages until either all description types have been read or all language
		// options exhausted
		val languageId = languageIds.mostPreferred
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Descriptions
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).withRoleIds(remainingRoleIds)
			.groupBy { _.targetId }
		
		// Reads the rest of the descriptions recursively
		readRemaining(remainingRoleIds, newAccessPoint, readDescriptions)
	}
	/**
	  * Reads description data from specified targets
	  * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	  * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	  * @param remainingRoleIds   Ids of the remaining description roles to read (shouldn't be empty)
	  * @param connection         DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
	@deprecated("Please use findInPreferredLanguages instead", "v1.3")
	protected def inLanguages(remainingTargetIds: Set[Int], languageIds: Seq[Int], remainingRoleIds: Set[Int])(
		implicit connection: Connection): Map[Int, Vector[DescriptionLinkOld]] =
		findInPreferredLanguages(remainingTargetIds, remainingRoleIds)(connection, LanguageIds(languageIds.toVector))
	
	/**
	  * Reads description data from specified targets
	  * @param roleId Id of the targeted role
	  * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	  * @param connection         DB Connection (implicit)
	  * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	  * @return Read descriptions, grouped by target id
	  */
	protected def withRoleIdInPreferredLanguages(roleId: Int, remainingTargetIds: Set[Int])
	                                            (implicit connection: Connection,
	                                             languageIds: LanguageIds): Map[Int, DescriptionLinkOld] =
	{
		// Reads descriptions in target languages until either all targets have been read or all language
		// options exhausted
		val languageId = languageIds.mostPreferred
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Description link
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).withRoleId(roleId)
			.map { link => link.targetId -> link }.toMap
		
		// Reads the rest of the descriptions recursively, if possible and necessary
		if (languageIds.size > 1)
		{
			val newRemainingTargetIds = remainingTargetIds -- readDescriptions.keySet
			if (remainingTargetIds.isEmpty)
				readDescriptions
			else
				readDescriptions ++
					withRoleIdInPreferredLanguages(roleId, newRemainingTargetIds)(connection, languageIds.tail)
		}
		else
			readDescriptions
	}
	/**
	 * Reads description data from specified targets
	 * @param roleId Id of the targeted role
	 * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	 * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	 * @param connection         DB Connection (implicit)
	 * @return Read descriptions, grouped by target id
	 */
	@deprecated("Please use withRoleInPreferredLanguages instead", "v1.3")
	protected def withRoleIdInLanguages(roleId: Int, remainingTargetIds: Set[Int], languageIds: Seq[Int])
	                                   (implicit connection: Connection): Map[Int, DescriptionLinkOld] =
		withRoleIdInPreferredLanguages(roleId, remainingTargetIds)(connection, LanguageIds(languageIds.toVector))
	
	// Continues read through recursion, if possible. Utilizes (and includes) existing read results.
	// LanguageIds and roles should be passed as they were at the start of the last read
	protected def readRemaining(remainingRoleIds: Set[Int], lastAccessPoint: DescriptionLinksForManyAccessLike,
	                          lastReadResults: Map[Int, Vector[DescriptionLinkOld]])
	                         (implicit connection: Connection, languageIds: LanguageIds): Map[Int, Vector[DescriptionLinkOld]] =
	{
		val remainingLanguageIds = languageIds.tail
		if (remainingLanguageIds.nonEmpty) {
			// Groups the remaining items based on the remaining roles without a description
			// Remaining roles -> target ids
			val remainingRolesWithTargets = lastReadResults.groupMap { case (_, descriptions) =>
				remainingRoleIds -- descriptions.map { _.description.roleId }
			} { case (targetId, _) => targetId }
				.filter { _._1.nonEmpty }
			
			// Performs additional queries for each remaining role group (provided there are languages left)
			if (remainingRolesWithTargets.isEmpty)
				lastReadResults
			else {
				val recursiveReadResults = remainingRolesWithTargets.map { case (roleIds, targetIds) =>
					lastAccessPoint.findInPreferredLanguages(targetIds.toSet, roleIds)(connection, remainingLanguageIds)
				}.reduce { _ ++ _ }
				lastReadResults ++ recursiveReadResults
			}
		}
		else
			lastReadResults
	}
}
