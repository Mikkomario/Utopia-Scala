package utopia.vault.test.database.access.item.versioned

import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows, TargetingTimeline}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.nosql.view.FilterableView
import utopia.vault.test.database.factory.item.VersionedTestItemDbFactory
import utopia.vault.test.database.storable.item.VersionedTestItemDbModel
import utopia.vault.test.model.stored.item.VersionedTestItem

import scala.language.implicitConversions

object AccessTestItems extends AccessManyRoot[AccessVersionedTestItemRows[VersionedTestItem]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to test items, including historical entries
	  */
	lazy val includingHistory = AccessVersionedTestItemRows(AccessManyRows(VersionedTestItemDbFactory))
	
	override lazy val root = includingHistory.active
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessTestItems[_, _]): AccessVersionedTestItemValues = access.values
}

/**
  * Used for accessing multiple test items from the DB at a time
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
abstract class AccessTestItems[A, +Repr <: AccessManyColumns with FilterableView[Repr]](wrapped: AccessManyColumns)
	extends TargetingTimeline[A, Repr, AccessVersionedTestItem[A]] with FilterTestItems[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible test items
	  */
	lazy val values = AccessVersionedTestItemValues(wrapped)
	
	override protected lazy val timestampColumn = VersionedTestItemDbModel.created
	
	
	// OTHER	--------------------
	
	/**
	  * Deprecates all accessible test items
	  * @param connection Implicit DB connection
	  * @return Whether any versioned test item was targeted
	  */
	def deprecate(implicit connection: Connection) = values.deprecationTimes.set(Now)
}

/**
  * Provides access to row-specific versioned test item -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class AccessVersionedTestItemRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessTestItems[A, AccessVersionedTestItemRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessVersionedTestItemRows[A], AccessVersionedTestItem[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessVersionedTestItemRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessVersionedTestItem(target)
}

/**
  * Used for accessing versioned test item items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class AccessCombinedTestItems[A](wrapped: TargetingMany[A]) 
	extends AccessTestItems[A, AccessCombinedTestItems[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedTestItems[A], AccessVersionedTestItem[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedTestItems(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessVersionedTestItem(target)
}

