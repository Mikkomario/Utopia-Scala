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
	  * @param errorId error id to target
	  * @return Copy of this access point that only includes issue variants with the specified error id
	  */
	def withError(errorId: Int) = filter(model.errorId.column <=> errorId)
	
	/**
	  * @param errorIds Targeted error ids
	  * @return Copy of this access point that only includes issue variants where error id is within the 
	  * specified value set
	  */
	def withErrors(errorIds: IterableOnce[Int]) = filter(model.errorId.column.in(IntSet.from(errorIds)))
}

