package utopia.logos.database.access.url.domain

import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

object AccessDomain extends AccessOneRoot[AccessDomain[Domain]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessDomains.root.head
}

/**
  * Used for accessing individual domains from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDomain[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessDomain[A]] with HasValues[AccessDomainValue] 
		with FilterDomains[AccessDomain[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible domain
	  */
	override lazy val values = AccessDomainValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessDomain(newTarget)
}

