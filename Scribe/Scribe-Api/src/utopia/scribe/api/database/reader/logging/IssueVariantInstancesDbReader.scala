package utopia.scribe.api.database.reader.logging

import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}
import utopia.vault.nosql.read.linked.MultiLinkedDbReader

/**
  * Used for reading issue variant instances from the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueVariantInstancesDbReader 
	extends MultiLinkedDbReader[IssueVariant, IssueOccurrence, IssueVariantInstances](
		IssueVariantDbReader, IssueOccurrenceDbReader)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param variant     variant to wrap
	  * @param occurrences occurrences to attach to this variant
	  */
	override def combine(variant: IssueVariant, occurrences: Seq[IssueOccurrence]) = 
		IssueVariantInstances(variant, occurrences.sorted)
}

