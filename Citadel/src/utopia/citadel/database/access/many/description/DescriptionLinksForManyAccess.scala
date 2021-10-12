package utopia.citadel.database.access.many.description

import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection

/**
  * A common trait for access points which provide description link access for multiple items at once
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1.0
  */
trait DescriptionLinksForManyAccess extends DescriptionLinksAccess
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @param remainingTargetIds Target item ids
	  * @return An access point that targets only those items
	  */
	protected def subGroup(remainingTargetIds: Set[Int]): DescriptionLinksForManyAccess
	
	
	// OTHER	------------------------------------
	
	/**
	  * Reads descriptions for these items using the specified languages. Only up to one description per
	  * role per target is read.
	  * @param languageIds Ids of the targeted languages, from most preferred to least preferred (less preferred
	  *                    language ids are used when no results can be found with the more preferred options)
	  * @param connection  DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
	def inLanguages(languageIds: Seq[Int])(implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
	{
		languageIds.headOption match
		{
			case Some(languageId: Int) =>
				// In the first iteration, reads all descriptions. After that divides into sub-groups
				val readDescriptions = inLanguageWithId(languageId).all.groupBy { _.targetId }
				// Reads the rest of the data using recursion
				readRemaining(languageIds, DbDescriptionRoleIds.all.toSet, this, readDescriptions)
			case None => Map()
		}
	}
	
	/**
	  * Reads description data from specified targets
	  * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	  * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	  * @param remainingRoleIds   Ids of the remaining description roles to read (shouldn't be empty)
	  * @param connection         DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
	protected def inLanguages(remainingTargetIds: Set[Int], languageIds: Seq[Int], remainingRoleIds: Set[Int])(
		implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
	{
		// Reads descriptions in target languages until either all description types have been read or all language
		// options exhausted
		val languageId = languageIds.head
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Descriptions
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).forRolesWithIds(remainingRoleIds)
			.groupBy { _.targetId }
		
		// Reads the rest of the descriptions recursively
		readRemaining(languageIds, remainingRoleIds, newAccessPoint, readDescriptions)
	}
	
	/**
	 * Reads description data from specified targets
	 * @param roleId Id of the targeted role
	 * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	 * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	 * @param connection         DB Connection (implicit)
	 * @return Read descriptions, grouped by target id
	 */
	protected def forRoleInLanguages(roleId: Int, remainingTargetIds: Set[Int], languageIds: Seq[Int])
	                                (implicit connection: Connection): Map[Int, DescriptionLink] =
	{
		// Reads descriptions in target languages until either all targets have been read or all language
		// options exhausted
		val languageId = languageIds.head
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Description link
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).forRoleWithId(roleId)
			.map { link => link.targetId -> link }.toMap
		
		// Reads the rest of the descriptions recursively, if possible and necessary
		if (languageIds.size > 1)
		{
			val newRemainingTargetIds = remainingTargetIds -- readDescriptions.keySet
			if (remainingTargetIds.isEmpty)
				readDescriptions
			else
				readDescriptions ++ forRoleInLanguages(roleId, newRemainingTargetIds, languageIds.tail)
		}
		else
			readDescriptions
	}
	
	// Continues read through recursion, if possible. Utilizes (and includes) existing read results.
	// LanguageIds and roles should be passed as they were at the start of the last read
	private def readRemaining(languageIds: Seq[Int],
	                          remainingRoleIds: Set[Int], lastAccessPoint: DescriptionLinksForManyAccess,
	                          lastReadResults: Map[Int, Vector[DescriptionLink]])
	                         (implicit connection: Connection): Map[Int, Vector[DescriptionLink]] =
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
					lastAccessPoint.inLanguages(targetIds.toSet, remainingLanguageIds, roleIds)
				}
					.reduce { _ ++ _ }
				lastReadResults ++ recursiveReadResults
			}
		}
		else
			lastReadResults
	}
}
