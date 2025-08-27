package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.stored.logging.{Issue, IssueOccurrence, IssueVariant}

object VaryingIssue
{
	// OTHER	--------------------
	
	/**
	  * @param issue    issue to wrap
	  * @param variants variants to attach to this issue
	  * @return Combination of the specified issue and variant
	  */
	def apply(issue: Issue, variants: Seq[IssueVariant]): VaryingIssue = _VaryingIssue(issue, variants)
	
	
	// NESTED	--------------------
	
	/**
	  * @param issue    issue to wrap
	  * @param variants variants to attach to this issue
	  */
	private case class _VaryingIssue(issue: Issue, variants: Seq[IssueVariant]) extends VaryingIssue
	{
		// IMPLEMENTED	--------------------
		
		override protected def wrap(factory: Issue) = copy(issue = factory)
	}
}

/**
  * Combines an issue with its different variants
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait VaryingIssue extends CombinedIssue[VaryingIssue]
{
	// ABSTRACT	--------------------
	
	/**
	  * Variants that are attached to this issue
	  */
	def variants: Seq[IssueVariant]
	
	
	// OTHER	--------------------
	
	/**
	  * @param occurrences Issue occurrences to attach (only those related to this issue will be
	  *                    included)
	  * @return Copy of this issue with occurrences included
	  */
	def withOccurrences(occurrences: Seq[IssueOccurrence]) = {
		val occurrencesByVariantId = occurrences.groupBy { _.caseId }
		IssueInstances(issue, variants.map { v => v.withOccurrences(occurrencesByVariantId.getOrElse(v.id, Empty)) })
	}
}

