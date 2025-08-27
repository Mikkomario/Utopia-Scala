package utopia.scribe.api.database.access.management.aliasing

import utopia.scribe.core.model.stored.management.IssueAlias
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

object AccessIssueAlias extends AccessOneRoot[AccessIssueAlias[IssueAlias]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessIssueAliases.root.head
}

/**
  * Used for accessing individual issue aliases from the DB at a time
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class AccessIssueAlias[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessIssueAlias[A]] with HasValues[AccessIssueAliasValue] 
		with FilterIssueAliases[AccessIssueAlias[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessIssueAliasValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssueAlias(newTarget)
}

