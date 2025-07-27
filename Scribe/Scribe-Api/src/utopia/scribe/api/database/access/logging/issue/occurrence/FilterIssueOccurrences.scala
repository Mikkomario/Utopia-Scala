package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.IssueOccurrenceDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on issue occurrence properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
trait FilterIssueOccurrences[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue occurrence database properties
	  */
	def model = IssueOccurrenceDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId case id to target
	  * @return Copy of this access point that only includes issue occurrences with the specified case id
	  */
	def ofVariant(caseId: Int) = filter(model.caseId.column <=> caseId)
	
	/**
	  * @param caseIds Targeted case ids
	  * @return Copy of this access point that only includes issue occurrences where case id is within the 
	  * specified value set
	  */
	def ofVariants(caseIds: IterableOnce[Int]) = filter(model.caseId.column.in(IntSet.from(caseIds)))
}

