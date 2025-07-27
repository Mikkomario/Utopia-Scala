package utopia.scribe.core.model.combined.logging

import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}

object ContextualIssueVariant
{
	// OTHER	--------------------
	
	/**
	  * @param variant variant to wrap
	  * @param issue   issue to attach to this variant
	  * @return Combination of the specified variant and issue
	  */
	def apply(variant: IssueVariant, issue: Issue): ContextualIssueVariant = 
		_ContextualIssueVariant(variant, issue)
	
	
	// NESTED	--------------------
	
	/**
	  * @param variant variant to wrap
	  * @param issue   issue to attach to this variant
	  */
	private case class _ContextualIssueVariant(variant: IssueVariant, issue: Issue) extends ContextualIssueVariant
	{
		// IMPLEMENTED	--------------------
		
		override protected def wrap(factory: IssueVariant) = copy(variant = factory)
	}
}

/**
  * Adds standard issue information to an issue variant
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait ContextualIssueVariant extends CombinedIssueVariant[ContextualIssueVariant]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped issue variant
	  */
	def variant: IssueVariant
	/**
	  * The issue that is attached to this variant
	  */
	def issue: Issue
	
	
	// IMPLEMENTED	--------------------
	
	override def issueVariant = variant
}
