package utopia.scribe.core.model.combined.logging

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}

/**
  * Adds specific occurrence/instance information to a single issue variant
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
case class IssueVariantInstances(variant: IssueVariant, occurrences: Vector[IssueOccurrence]) 
	extends Extender[IssueVariantData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this variant in the database
	  */
	def id = variant.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = variant.data
}

