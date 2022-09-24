package utopia.citadel.database.access.many.language

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbLanguageFamiliarityDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.language.LanguageFamiliarityModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyLanguageFamiliaritiesAccess
{
	// NESTED	--------------------
	
	private class ManyLanguageFamiliaritiesSubView(override val parent: ManyRowModelAccess[LanguageFamiliarity], 
		override val filterCondition: Condition) 
		extends ManyLanguageFamiliaritiesAccess with SubView
}

/**
  * A common trait for access points which target multiple LanguageFamiliarities at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
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
	
	override def filter(additionalCondition: Condition): ManyLanguageFamiliaritiesAccess = 
		new ManyLanguageFamiliaritiesAccess.ManyLanguageFamiliaritiesSubView(this, additionalCondition)
	
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

