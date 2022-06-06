package utopia.exodus.model.stored.auth

import utopia.citadel.database.access.single.user.DbUserSettings
import utopia.exodus.database.access.single.auth.DbSingleToken
import utopia.exodus.model.combined.auth.{EmailValidationToken, ScopedToken, TypedToken}
import utopia.exodus.model.partial.auth.TokenData
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.Connection
import utopia.vault.model.template.Stored

/**
  * Represents a token that has already been stored in the database
  * @param id id of this token in the database
  * @param data Wrapped token data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class Token(id: Int, data: TokenData) extends Stored[TokenData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token in the database
	  */
	def access = DbSingleToken(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return A copy of this token which includes scope link information (from DB)
	  */
	def withScopeLinksPulled(implicit connection: Connection) = withScopeLinks(access.scopeLinks.pull)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return An iterator that keeps pulling parent token information until it reaches the root token
	  */
	def pullParentsIterator(implicit connection: Connection) =
		Iterator.iterate[Option[Token]](Some(this)) { _.flatMap { _.parentAccess.flatMap { _.pull } } }
			.drop(1).takeWhile { _.isDefined }.map { _.get }
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Finds linked email validation attempt's listed email address. Searches from this token, as well as from
	  *         any parent token, until a validation attempt is found or until the whole token hierarchy has been
	  *         searched
	  */
	def pullValidatedEmailAddress(implicit connection: Connection) =
		access.emailValidationAttempt.emailAddress
			.orElse { pullParentsIterator.findMap { _.access.emailValidationAttempt.emailAddress } }
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Id of this token's owner, either directly or via an email validation
	  */
	def pullOwnerId(implicit connection: Connection) = data.ownerId
		.orElse { pullValidatedEmailAddress.flatMap { DbUserSettings.withEmail(_).userId } }
	
	
	// OTHER    ---------------------
	
	/**
	  * @param tokenType Type information to include
	  * @return A copy of this token with that type information included
	  */
	def withTypeInfo(tokenType: TokenType) = TypedToken(this, tokenType)
	
	/**
	  * @param scopeLinks Scope links to attach to this token
	  * @return A copy of this token which includes the specified token scope links
	  */
	def withScopeLinks(scopeLinks: Vector[TokenScopeLink]) = ScopedToken(this, scopeLinks)
	
	/**
	  * @param attempt Email validation attempt data
	  * @return A combination of this token and the specified email validation attempt data
	  */
	def withEmailValidationAttempt(attempt: EmailValidationAttempt) = EmailValidationToken(this, attempt)
}

