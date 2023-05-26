package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.{Issue, IssueOccurrence, IssueVariant}

/**
  * Combines an issue with its different variants
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
case class VaryingIssue(issue: Issue, variants: Vector[IssueVariant]) extends Extender[IssueData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this issue in the database
	  */
	def id = issue.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = issue.data
	
	
	// OTHER    -----------------------
	
	/**
	  * @param occurrences Issue occurrences to attach (only those related to this issue will be included)
	  * @return Copy of this issue with occurrences included
	  */
	def withOccurrences(occurrences: Vector[IssueOccurrence]) = {
		val occurrencesByVariantId = occurrences.groupBy { _.caseId }
		IssueInstances(issue, variants.map { v =>
			v.withOccurrences(occurrencesByVariantId.getOrElse(v.id, Vector.empty))
		})
	}
}

