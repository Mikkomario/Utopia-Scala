package utopia.vault.test.database.access.item.versioned

import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vault.test.model.stored.item.VersionedTestItem

import scala.language.implicitConversions

object AccessVersionedTestItem extends AccessOneRoot[AccessVersionedTestItem[VersionedTestItem]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessTestItems.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessVersionedTestItem[_]): AccessVersionedTestItemValue = 
		access.values
}

/**
  * Used for accessing individual test items from the DB at a time
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class AccessVersionedTestItem[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessVersionedTestItem[A]] 
		with FilterTestItems[AccessVersionedTestItem[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible versioned test item
	  */
	lazy val values = AccessVersionedTestItemValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessVersionedTestItem(newTarget)
}

