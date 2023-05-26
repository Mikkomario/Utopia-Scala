package utopia.scribe.api.database.factory.logging

import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory

/**
  * Used for reading issue variant instances from the database
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
object IssueVariantInstancesFactory 
	extends MultiCombiningFactory[IssueVariantInstances, IssueVariant, IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = IssueOccurrenceFactory
	
	override def isAlwaysLinked = true
	
	override def parentFactory = IssueVariantFactory
	
	override def apply(variant: IssueVariant, occurrences: Vector[IssueOccurrence]) = 
		IssueVariantInstances(variant, occurrences)
}

