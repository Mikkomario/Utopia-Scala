package utopia.logos.database.access.text.statement

import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessStatement extends AccessOneRoot[AccessStatement[StoredStatement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessStatements.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStatement[_]): AccessStatementValue = access.values
}

/**
  * Used for accessing individual statements from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessStatement[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessStatement[A]] with FilterStatements[AccessStatement[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible statement
	  */
	lazy val values = AccessStatementValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessStatement(newTarget)
}

