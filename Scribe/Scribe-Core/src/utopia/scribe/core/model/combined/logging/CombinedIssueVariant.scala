package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.factory.logging.IssueVariantFactoryWrapper
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.IssueVariant

/**
  * Common trait for combinations that add additional data to issue variants
  * @tparam Repr Type of the implementing class
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait CombinedIssueVariant[+Repr] 
	extends Extender[IssueVariantData] with IssueVariantFactoryWrapper[IssueVariant, Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped issue variant
	  */
	def issueVariant: IssueVariant
	
	
	// COMPUTED	--------------------
	
	/**
	  * Id of this issue variant in the database
	  */
	def id = issueVariant.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = issueVariant.data
	
	override protected def wrappedFactory = issueVariant
}

