package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.Token
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing TokenModel instances and for inserting tokens to the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenModel extends DataInserter[TokenModel, Token, TokenData] with DeprecatableAfter[TokenModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains token type id
	  */
	val typeIdAttName = "typeId"
	
	/**
	  * Name of the property that contains token hash
	  */
	val hashAttName = "hash"
	
	/**
	  * Name of the property that contains token parent token id
	  */
	val parentTokenIdAttName = "parentTokenId"
	
	/**
	  * Name of the property that contains token owner id
	  */
	val ownerIdAttName = "ownerId"
	
	/**
	  * Name of the property that contains token model style preference
	  */
	val modelStylePreferenceAttName = "modelStyleId"
	
	/**
	  * Name of the property that contains token expires
	  */
	val expiresAttName = "expires"
	
	/**
	  * Name of the property that contains token created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains token deprecated after
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	/**
	  * Name of the property that contains token is single use only
	  */
	val isSingleUseOnlyAttName = "isSingleUseOnly"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains token type id
	  */
	def typeIdColumn = table(typeIdAttName)
	
	/**
	  * Column that contains token hash
	  */
	def hashColumn = table(hashAttName)
	
	/**
	  * Column that contains token parent token id
	  */
	def parentTokenIdColumn = table(parentTokenIdAttName)
	
	/**
	  * Column that contains token owner id
	  */
	def ownerIdColumn = table(ownerIdAttName)
	
	/**
	  * Column that contains token model style preference
	  */
	def modelStylePreferenceColumn = table(modelStylePreferenceAttName)
	
	/**
	  * Column that contains token expires
	  */
	def expiresColumn = table(expiresAttName)
	
	/**
	  * Column that contains token created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains token deprecated after
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * Column that contains token is single use only
	  */
	def isSingleUseOnlyColumn = table(isSingleUseOnlyAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TokenFactory
	
	
	// IMPLEMENTED	--------------------
	
	// Also requires the token not to be expired
	override def nonDeprecatedCondition = {
		val expCol = expiresColumn
		super.nonDeprecatedCondition && (expCol.isNull || expiresColumn > Now)
	}
	
	override def table = factory.table
	
	override def apply(data: TokenData) = 
		apply(None, Some(data.typeId), Some(data.hash), data.parentTokenId, data.ownerId,
			data.modelStylePreference, data.expires, Some(data.created), data.deprecatedAfter, 
			Some(data.isSingleUseOnly))
	
	override def complete(id: Value, data: TokenData) = Token(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this token was issued
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this token was revoked or replaced
	  * @return A model containing only the specified deprecated after
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param expires Time when this token expires, if applicable
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	
	/**
	  * @param hash A hashed version of this token
	  * @return A model containing only the specified hash
	  */
	def withHash(hash: String) = apply(hash = Some(hash))
	
	/**
	  * @param id A token id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param isSingleUseOnly Whether this token may only be used once (successfully)
	  * @return A model containing only the specified is single use only
	  */
	def withIsSingleUseOnly(isSingleUseOnly: Boolean) = apply(isSingleUseOnly = Some(isSingleUseOnly))
	
	/**
	  * @param modelStylePreference Model style preferred during this session
	  * @return A model containing only the specified model style preference
	  */
	def withModelStylePreference(modelStylePreference: ModelStyle) = 
		apply(modelStylePreference = Some(modelStylePreference))
	
	/**
	  * @param ownerId Id of the user who owns this token, if applicable
	  * @return A model containing only the specified owner id
	  */
	def withOwnerId(ownerId: Int) = apply(ownerId = Some(ownerId))
	
	/**
	  * @param parentTokenId Id of the token that was used to acquire this token, if applicable & still known
	  * @return A model containing only the specified parent token id
	  */
	def withParentTokenId(parentTokenId: Int) = apply(parentTokenId = Some(parentTokenId))
	
	/**
	  * @param typeId Id of the token type applicable to this token
	  * @return A model containing only the specified type id
	  */
	def withTypeId(typeId: Int) = apply(typeId = Some(typeId))
}

/**
  * Used for interacting with Tokens in the database
  * @param id token database id
  * @param typeId Id of the token type applicable to this token
  * @param hash A hashed version of this token
  * @param parentTokenId Id of the token that was used to acquire this token, if applicable & still known
  * @param ownerId Id of the user who owns this token, if applicable
  * @param modelStylePreference Model style preferred during this session
  * @param expires Time when this token expires, if applicable
  * @param created Time when this token was issued
  * @param deprecatedAfter Time when this token was revoked or replaced
  * @param isSingleUseOnly Whether this token may only be used once (successfully)
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenModel(id: Option[Int] = None, typeId: Option[Int] = None, hash: Option[String] = None, 
	parentTokenId: Option[Int] = None, ownerId: Option[Int] = None,
	modelStylePreference: Option[ModelStyle] = None, expires: Option[Instant] = None, 
	created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None, 
	isSingleUseOnly: Option[Boolean] = None) 
	extends StorableWithFactory[Token]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TokenModel.factory
	
	override def valueProperties = {
		import TokenModel._
		Vector("id" -> id, typeIdAttName -> typeId, hashAttName -> hash, 
			parentTokenIdAttName -> parentTokenId, ownerIdAttName -> ownerId,
			modelStylePreferenceAttName -> modelStylePreference.map { _.id }, expiresAttName -> expires, 
			createdAttName -> created, deprecatedAfterAttName -> deprecatedAfter, 
			isSingleUseOnlyAttName -> isSingleUseOnly)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecated after
	  * @return A new copy of this model with the specified deprecated after
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param hash A new hash
	  * @return A new copy of this model with the specified hash
	  */
	def withHash(hash: String) = copy(hash = Some(hash))
	
	/**
	  * @param isSingleUseOnly A new is single use only
	  * @return A new copy of this model with the specified is single use only
	  */
	def withIsSingleUseOnly(isSingleUseOnly: Boolean) = copy(isSingleUseOnly = Some(isSingleUseOnly))
	
	/**
	  * @param modelStylePreference A new model style preference
	  * @return A new copy of this model with the specified model style preference
	  */
	def withModelStylePreference(modelStylePreference: ModelStyle) = 
		copy(modelStylePreference = Some(modelStylePreference))
	
	/**
	  * @param ownerId A new owner id
	  * @return A new copy of this model with the specified owner id
	  */
	def withOwnerId(ownerId: Int) = copy(ownerId = Some(ownerId))
	
	/**
	  * @param parentTokenId A new parent token id
	  * @return A new copy of this model with the specified parent token id
	  */
	def withParentTokenId(parentTokenId: Int) = copy(parentTokenId = Some(parentTokenId))
	
	/**
	  * @param typeId A new type id
	  * @return A new copy of this model with the specified type id
	  */
	def withTypeId(typeId: Int) = copy(typeId = Some(typeId))
}

