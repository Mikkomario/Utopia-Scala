package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.{DbLanguageFamiliarityDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.language.LanguageFamiliarityModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyLanguageFamiliaritiesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyLanguageFamiliaritiesAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyLanguageFamiliaritiesAccess
}

/**
  * A common trait for access points which target multiple LanguageFamiliarities at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyLanguageFamiliaritiesAccess 
	extends ManyRowModelAccess[LanguageFamiliarity] 
		with ManyDescribedAccess[LanguageFamiliarity, DescribedLanguageFamiliarity] 
		with FilterableView[ManyLanguageFamiliaritiesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * orderIndexs of the accessible LanguageFamiliarities
	  */
	def orderIndexs(implicit connection: Connection) = 
		pullColumn(model.orderIndexColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible LanguageFamiliarities
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageFamiliarityModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFamiliarityFactory
	
	override protected def describedFactory = DescribedLanguageFamiliarity
	
	override protected def manyDescriptionsAccess = DbLanguageFamiliarityDescriptions
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyLanguageFamiliaritiesAccess = 
		ManyLanguageFamiliaritiesAccess(condition)
	
	override def idOf(item: LanguageFamiliarity) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted LanguageFamiliarity instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any LanguageFamiliarity instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the orderIndex of the targeted LanguageFamiliarity instance(s)
	  * @param newOrderIndex A new orderIndex to assign
	  * @return Whether any LanguageFamiliarity instance was affected
	  */
	def orderIndexs_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndexColumn, newOrderIndex)
}

