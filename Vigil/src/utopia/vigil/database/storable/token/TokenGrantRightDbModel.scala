package utopia.vigil.database.storable.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.UncertainBoolean
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.VigilTables
import utopia.vigil.model.factory.token.TokenGrantRightFactory
import utopia.vigil.model.partial.token.TokenGrantRightData
import utopia.vigil.model.stored.token.TokenGrantRight

/**
  * Used for constructing TokenGrantRightDbModel instances and for inserting token grant rights 
  * to the database
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
object TokenGrantRightDbModel 
	extends StorableFactory[TokenGrantRightDbModel, TokenGrantRight, TokenGrantRightData] 
		with FromIdFactory[Int, TokenGrantRightDbModel] with HasIdProperty 
		with TokenGrantRightFactory[TokenGrantRightDbModel]
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with owner template ids
	  */
	lazy val ownerTemplateId = property("ownerTemplateId")
	
	/**
	  * Database property used for interacting with granted template ids
	  */
	lazy val grantedTemplateId = property("grantedTemplateId")
	
	/**
	  * Database property used for interacting with revoke originals
	  */
	lazy val revokesOriginal = property("revokesOriginal")
	
	/**
	  * Database property used for interacting with revoke earlier
	  */
	lazy val revokesEarlier = property("revokesEarlier")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.tokenGrantRight
	
	override def apply(data: TokenGrantRightData): TokenGrantRightDbModel = 
		apply(None, Some(data.ownerTemplateId), Some(data.grantedTemplateId), Some(data.revokesOriginal), 
			data.revokesEarlier.exact)
	
	override def withGrantedTemplateId(grantedTemplateId: Int) = 
		apply(grantedTemplateId = Some(grantedTemplateId))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withOwnerTemplateId(ownerTemplateId: Int) = apply(ownerTemplateId = Some(ownerTemplateId))
	
	override def withRevokesEarlier(revokesEarlier: UncertainBoolean) = 
		apply(revokesEarlier = revokesEarlier.exact)
	
	override def withRevokesOriginal(revokesOriginal: Boolean) = apply(revokesOriginal = 
		Some(revokesOriginal))
	
	override protected def complete(id: Value, data: TokenGrantRightData) = TokenGrantRight(id.getInt, data)
}

/**
  * Used for interacting with TokenGrantRights in the database
  * @param id token grant right database id
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
case class TokenGrantRightDbModel(id: Option[Int] = None, ownerTemplateId: Option[Int] = None, 
	grantedTemplateId: Option[Int] = None, revokesOriginal: Option[Boolean] = None, 
	revokesEarlier: Option[Boolean] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, TokenGrantRightDbModel] 
		with TokenGrantRightFactory[TokenGrantRightDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(TokenGrantRightDbModel.id.name -> id, 
			TokenGrantRightDbModel.ownerTemplateId.name -> ownerTemplateId, 
			TokenGrantRightDbModel.grantedTemplateId.name -> grantedTemplateId, 
			TokenGrantRightDbModel.revokesOriginal.name -> revokesOriginal, 
			TokenGrantRightDbModel.revokesEarlier.name -> revokesEarlier)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = TokenGrantRightDbModel.table
	
	override def withGrantedTemplateId(grantedTemplateId: Int) = copy(grantedTemplateId = 
		Some(grantedTemplateId))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withOwnerTemplateId(ownerTemplateId: Int) = copy(ownerTemplateId = Some(ownerTemplateId))
	
	override def withRevokesEarlier(revokesEarlier: UncertainBoolean) = 
		copy(revokesEarlier = revokesEarlier.exact)
	
	override def withRevokesOriginal(revokesOriginal: Boolean) = copy(revokesOriginal = Some(revokesOriginal))
}

