package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

object ManyIssueVariantsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyIssueVariantsAccess = new ManyIssueVariantsSubView(condition)
	
	
	// NESTED	--------------------
	
	private class ManyIssueVariantsSubView(condition: Condition) extends ManyIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issue variants at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssueVariantsAccess 
	extends ManyIssueVariantsAccessLike[IssueVariant, ManyIssueVariantsAccess] 
		with ManyRowModelAccess[IssueVariant] with ChronoRowFactoryView[IssueVariant, ManyIssueVariantsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Copy of this access point, which includes issue information
	  */
	def contextual = DbContextualIssueVariants.filter(accessCondition)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyIssueVariantsAccess = ManyIssueVariantsAccess(condition)
}

