package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.citadel.database.model.description.DescriptionRoleModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyDescriptionRolesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyDescriptionRolesAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyDescriptionRolesAccess
}

/**
  * A common trait for access points which target multiple DescriptionRoles at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyDescriptionRolesAccess 
	extends ManyRowModelAccess[DescriptionRole] 
		with ManyDescribedAccess[DescriptionRole, DescribedDescriptionRole] 
		with FilterableView[ManyDescriptionRolesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * singularJsonKeys of the accessible DescriptionRoles
	  */
	def singularJsonKeys(implicit connection: Connection) = 
		pullColumn(model.jsonKeySingularColumn).flatMap { value => value.string }
	
	/**
	  * pluralJsonKeys of the accessible DescriptionRoles
	  */
	def pluralJsonKeys(implicit connection: Connection) = 
		pullColumn(model.jsonKeyPluralColumn).flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible DescriptionRoles
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionRoleFactory
	
	override protected def describedFactory = DescribedDescriptionRole
	
	override protected def manyDescriptionsAccess = DbDescriptionRoleDescriptions
	
	override protected def self = this
	
	override
		 def apply(condition: Condition): ManyDescriptionRolesAccess = ManyDescriptionRolesAccess(condition)
	
	override def idOf(item: DescriptionRole) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted DescriptionRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the jsonKeyPlural of the targeted DescriptionRole instance(s)
	  * @param newJsonKeyPlural A new jsonKeyPlural to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def pluralJsonKeys_=(newJsonKeyPlural: String)(implicit connection: Connection) = 
		putColumn(model.jsonKeyPluralColumn, newJsonKeyPlural)
	
	/**
	  * Updates the jsonKeySingular of the targeted DescriptionRole instance(s)
	  * @param newJsonKeySingular A new jsonKeySingular to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def singularJsonKeys_=(newJsonKeySingular: String)(implicit connection: Connection) = 
		putColumn(model.jsonKeySingularColumn, newJsonKeySingular)
}

