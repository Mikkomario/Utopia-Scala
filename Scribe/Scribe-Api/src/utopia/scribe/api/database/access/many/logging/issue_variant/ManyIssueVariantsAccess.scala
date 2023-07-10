package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyIssueVariantsAccess
{
	// NESTED	--------------------
	
	private class ManyIssueVariantsSubView(condition: Condition) extends ManyIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issue variants at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssueVariantsAccess 
	extends ManyIssueVariantsAccessLike[IssueVariant, ManyIssueVariantsAccess] 
		with ManyRowModelAccess[IssueVariant] 
		with ChronoRowFactoryView[IssueVariant, ManyIssueVariantsAccess]
{
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this access point, which includes issue information
	  */
	def contextual = DbContextualIssueVariants.filter(globalCondition)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyIssueVariantsAccess = 
		new ManyIssueVariantsAccess.ManyIssueVariantsSubView(mergeCondition(filterCondition))
}

