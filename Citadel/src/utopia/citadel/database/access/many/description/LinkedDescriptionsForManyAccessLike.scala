package utopia.citadel.database.access.many.description

import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.vault.database.Connection

/**
  * A common trait for access points which provide linked description access for multiple items at once
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
trait LinkedDescriptionsForManyAccessLike extends LinkedDescriptionsAccessLike
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @param remainingTargetIds Target item ids
	  * @return An access point that targets only those items
	  */
	protected def subGroup(remainingTargetIds: Set[Int]): LinkedDescriptionsForManyAccessLike
	
	
	// OTHER	------------------------------------
	
	/**
	  * Reads description data from specified targets
	  * @param remainingTargetIds Targeted target ids (shouldn't be empty)
	  * @param remainingRoleIds   Ids of the remaining description roles to read (shouldn't be empty)
	  * @param languageIds        Ids of the languages to use, from most to least preferred (mustn't be empty)
	  * @param connection         DB Connection (implicit)
	  * @return Read descriptions, grouped by target id
	  */
	protected def findInPreferredLanguages(remainingTargetIds: Set[Int], remainingRoleIds: Set[Int])(
		implicit connection: Connection, languageIds: LanguageIds): Map[Int, Seq[LinkedDescription]] =
	{
		// Reads descriptions in target languages until either all description types have been read or all language
		// options exhausted
		val languageId = languageIds.mostPreferred
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Descriptions
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).withRoleIds(remainingRoleIds).pull
			.groupBy { _.targetId }
		
		// Reads the rest of the descriptions recursively
		readRemaining(remainingRoleIds, newAccessPoint, readDescriptions)
	}
	
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
	                                             languageIds: LanguageIds): Map[Int, LinkedDescription] =
	{
		// Reads descriptions in target languages until either all targets have been read or all language
		// options exhausted
		val languageId = languageIds.mostPreferred
		val newAccessPoint = subGroup(remainingTargetIds)
		// Target id -> Description link
		val readDescriptions = newAccessPoint.inLanguageWithId(languageId).withRoleId(roleId).toMapBy { _.targetId }
		
		// Reads the rest of the descriptions recursively, if possible and necessary
		if (languageIds.size > 1) {
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
	// Continues read through recursion, if possible. Utilizes (and includes) existing read results.
	// LanguageIds and roles should be passed as they were at the start of the last read
	protected def readRemaining(remainingRoleIds: Set[Int], lastAccessPoint: LinkedDescriptionsForManyAccessLike,
	                            lastReadResults: Map[Int, Seq[LinkedDescription]])
	                           (implicit connection: Connection,
	                            languageIds: LanguageIds): Map[Int, Seq[LinkedDescription]] =
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
