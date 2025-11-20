package utopia.logos.database.access.text.statement

import utopia.logos.database.factory.text.StatementDbFactory
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessStatements extends AccessManyRoot[AccessStatements[StoredStatement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(StatementDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStatements[_]): AccessStatementValues = access.values
}

/**
  * Used for accessing multiple statements from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessStatements[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessStatements[A], AccessStatement[A]] 
		with FilterStatements[AccessStatements[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible statements
	  */
	lazy val values = AccessStatementValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessStatements(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessStatement(target)
}

