package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.{Issue, IssueVariant}

/**
  * Adds standard issue information to an issue variant
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
case class ContextualIssueVariant(variant: IssueVariant, issue: Issue) extends Extender[IssueVariantData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this variant in the database
	  */
	def id = variant.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = variant.data
}

