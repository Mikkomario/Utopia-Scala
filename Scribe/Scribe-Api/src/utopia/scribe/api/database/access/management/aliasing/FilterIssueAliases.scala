package utopia.scribe.api.database.access.management.aliasing

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueAliasDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on issue alias properties
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
trait FilterIssueAliases[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue alias database properties
	  */
	def model = IssueAliasDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param issueId issue id to target
	  * @return Copy of this access point that only includes issue aliases with the specified issue id
	  */
	def ofIssue(issueId: Int) = filter(model.issueId.column <=> issueId)
	
	/**
	  * @param issueIds Targeted issue ids
	  * @return Copy of this access point that only includes issue aliases where issue id is within the 
	  * specified value set
	  */
	def ofIssues(issueIds: IterableOnce[Int]) = filter(model.issueId.column.in(IntSet.from(issueIds)))
}

