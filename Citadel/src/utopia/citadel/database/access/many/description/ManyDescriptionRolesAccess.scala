package utopia.citadel.database.access.many.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.citadel.database.model.description.DescriptionRoleModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyDescriptionRolesAccess
{
	// NESTED	--------------------
	
	private class ManyDescriptionRolesSubView(override val parent: ManyRowModelAccess[DescriptionRole], 
		override val filterCondition: Condition) 
		extends ManyDescriptionRolesAccess with SubView
}

/**
  * A common trait for access points which target multiple DescriptionRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
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
	
	override def filter(additionalCondition: Condition): ManyDescriptionRolesAccess = 
		new ManyDescriptionRolesAccess.ManyDescriptionRolesSubView(this, additionalCondition)
	
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

