package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.immutable.Empty
import utopia.scribe.core.model.stored.logging.{IssueOccurrence, IssueVariant}

/**
  * Adds error and occurrence information to an issue variant
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Combines issue variant, error and occurrence data
  * @param variant The issue variant to wrap
  * @param error The error associated with this issue variant
  * @param occurrences Occurrences of this issue variant
  */
case class DetailedIssueVariant(variant: IssueVariant, error: Option[ErrorRecordWithStackTrace] = None,
                                occurrences: Seq[IssueOccurrence] = Empty)
	extends IssueVariantInstances with CombinedIssueVariant[DetailedIssueVariant]
{
	// IMPLEMENTED  ------------------------
	
	override protected def wrap(factory: IssueVariant): DetailedIssueVariant = copy(variant = factory)
}