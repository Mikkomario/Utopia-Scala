package utopia.scribe.api.database.access.logging.issue.variant

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.logging.IssueVariantDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on issue variant properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
trait FilterIssueVariants[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue variant database properties
	  */
	def model = IssueVariantDbModel
	
	/**
	  * @return Access to variants not caused by any error
	  */
	def notCausedByError = filter(model.errorId.isNull)
	
	
	// OTHER	--------------------
	
	/**
	  * @param version version to target
	  * @return Copy of this access point that only includes issue variants with the specified version
	  */
	def affectingVersion(version: Version) = filter(model.version.column <=> version.toString)
	/**
	  * @param versions Targeted versions
	  * @return Copy of this access point that only includes issue variants where version is within the 
	  * specified value set
	  */
	def affectingVersions(versions: Iterable[Version]) = 
		filter(model.version.column.in(versions.map { version => version.toString }))
	
	/**
	  * @param issueId issue id to target
	  * @return Copy of this access point that only includes issue variants with the specified issue id
	  */
	def ofIssue(issueId: Int) = filter(model.issueId.column <=> issueId)
	/**
	  * @param issueIds Targeted issue ids
	  * @return Copy of this access point that only includes issue variants where issue id is within the 
	  * specified value set
	  */
	def ofIssues(issueIds: IterableOnce[Int]) = filter(model.issueId.column.in(IntSet.from(issueIds)))
	/**
	 * @param issueIds Targeted issue IDs
	 * @param maxConditions Maximum number of generated conditions / comparisons
	 * @return If the specified ID set is small enough, filters to only that set. Otherwise, performs no filtering.
	 */
	def ofLimitedIssues(issueIds: IterableOnce[Int], maxConditions: Int) =
		filter(model.issueId.inIfLimited(IntSet.from(issueIds), maxConditions))
	
	/**
	  * @param errorId error id to target
	  * @return Copy of this access point that only includes issue variants with the specified error id
	  */
	def causedBy(errorId: Int) = filter(model.errorId.column <=> errorId)
	/**
	  * @param errorId Id of the targeted error
	  * @return Copy of this access limited to cases caused by the specified error.
	  *         If 'errorId' was None, limits this access to cases not caused by any error.
	  */
	def causedBy(errorId: Option[Int]): Repr = errorId match {
		case Some(errorId) => causedBy(errorId)
		case None => notCausedByError
	}
	/**
	  * @param errorIds Targeted error ids
	  * @return Copy of this access point that only includes issue variants where error id is within the 
	  * specified value set
	  */
	def causedByErrors(errorIds: IterableOnce[Int]) = filter(model.errorId.column.in(IntSet.from(errorIds)))
}

