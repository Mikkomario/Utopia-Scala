package utopia.exodus.database.model.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.exodus.database.factory.auth.TokenTypeFactory
import utopia.exodus.model.partial.auth.TokenTypeData
import utopia.exodus.model.stored.auth.TokenType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing TokenTypeModel instances and for inserting token types to the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenTypeModel extends DataInserter[TokenTypeModel, TokenType, TokenTypeData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains token type name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains token type duration
	  */
	val durationAttName = "durationMinutes"
	
	/**
	  * Name of the property that contains token type refreshed type id
	  */
	val refreshedTypeIdAttName = "refreshedTypeId"
	
	/**
	  * Name of the property that contains token type created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains token type is single use only
	  */
	val isSingleUseOnlyAttName = "isSingleUseOnly"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains token type name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains token type duration
	  */
	def durationColumn = table(durationAttName)
	
	/**
	  * Column that contains token type refreshed type id
	  */
	def refreshedTypeIdColumn = table(refreshedTypeIdAttName)
	
	/**
	  * Column that contains token type created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains token type is single use only
	  */
	def isSingleUseOnlyColumn = table(isSingleUseOnlyAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TokenTypeFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: TokenTypeData) = 
		apply(None, Some(data.name), data.duration, data.refreshedTypeId, Some(data.created), 
			Some(data.isSingleUseOnly))
	
	override def complete(id: Value, data: TokenTypeData) = TokenType(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this token type was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param duration Duration that determines how long these tokens remain valid after issuing. None if
	  *  these tokens don't expire automatically.
	  * @return A model containing only the specified duration
	  */
	def withDuration(duration: FiniteDuration) = apply(duration = Some(duration))
	
	/**
	  * @param id A token type id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param isSingleUseOnly Whether tokens of this type may only be used once (successfully)
	  * @return A model containing only the specified is single use only
	  */
	def withIsSingleUseOnly(isSingleUseOnly: Boolean) = apply(isSingleUseOnly = Some(isSingleUseOnly))
	
	/**
	  * @param name Name of this token type for identification. Not localized.
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * @param refreshedTypeId Id of the type of token that may be acquired by using this token type as a refresh token,
	  * if applicable
	  * @return A model containing only the specified refreshed type id
	  */
	def withRefreshedTypeId(refreshedTypeId: Int) = apply(refreshedTypeId = Some(refreshedTypeId))
}

/**
  * Used for interacting with TokenTypes in the database
  * @param id token type database id
  * @param name Name of this token type for identification. Not localized.
  * @param duration Duration that determines how long these tokens remain valid after issuing. None if
  *  these tokens don't expire automatically.
  * @param refreshedTypeId Id of the type of token that may be acquired by using this token type as a refresh token, 
	
  * if applicable
  * @param created Time when this token type was first created
  * @param isSingleUseOnly Whether tokens of this type may only be used once (successfully)
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenTypeModel(id: Option[Int] = None, name: Option[String] = None, 
	duration: Option[FiniteDuration] = None, refreshedTypeId: Option[Int] = None, 
	created: Option[Instant] = None, isSingleUseOnly: Option[Boolean] = None) 
	extends StorableWithFactory[TokenType]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TokenTypeModel.factory
	
	override def valueProperties = {
		import TokenTypeModel._
		Vector("id" -> id, nameAttName -> name, 
			durationAttName -> duration.map { _.toUnit(TimeUnit.MINUTES) }, 
			refreshedTypeIdAttName -> refreshedTypeId, createdAttName -> created, 
			isSingleUseOnlyAttName -> isSingleUseOnly)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param duration A new duration
	  * @return A new copy of this model with the specified duration
	  */
	def withDuration(duration: FiniteDuration) = copy(duration = Some(duration))
	
	/**
	  * @param isSingleUseOnly A new is single use only
	  * @return A new copy of this model with the specified is single use only
	  */
	def withIsSingleUseOnly(isSingleUseOnly: Boolean) = copy(isSingleUseOnly = Some(isSingleUseOnly))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
	
	/**
	  * @param refreshedTypeId A new refreshed type id
	  * @return A new copy of this model with the specified refreshed type id
	  */
	def withRefreshedTypeId(refreshedTypeId: Int) = copy(refreshedTypeId = Some(refreshedTypeId))
}

