package utopia.logos.database.access.text.statement

import utopia.logos.database.reader.text.StatementDbReader
import utopia.logos.database.storable.text.StatementDbModel
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, TargetingTimeline}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessStatements extends AccessManyRoot[AccessStatementRows[StoredStatement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessStatementRows(AccessManyRows(StatementDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessStatements[_, _]): AccessStatementValues = access.values
}

/**
  * Used for accessing multiple statements from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessStatements[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessStatement[A]] with FilterStatements[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible statements
	  */
	lazy val values = AccessStatementValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def timestamp = StatementDbModel.created
}

/**
  * Provides access to row-specific statement -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessStatementRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessStatements[A, AccessStatementRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessStatementRows[A], AccessStatement[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessStatementRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessStatement(target)
}

/**
  * Used for accessing statement items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedStatements[A](wrapped: TargetingMany[A]) 
	extends AccessStatements[A, AccessCombinedStatements[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedStatements[A], AccessStatement[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedStatements(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessStatement(target)
}

