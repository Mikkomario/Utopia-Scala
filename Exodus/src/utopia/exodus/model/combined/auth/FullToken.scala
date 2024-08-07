package utopia.exodus.model.combined.auth

import utopia.exodus.model.stored.auth.{Token, TokenType}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Constant
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}

/**
  * Adds token type and scope information to a token
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
case class FullToken(token: Token, tokenType: TokenType, scopes: Seq[TokenScope]) extends ScopedTokenLike
{
	// IMPLEMENTED  -------------------------
	
	override def scopeLinks = scopes.map { _.tokenLink }
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a model based on this token information
	  * @param tokenString A non-hashed version of this token
	  * @param style Styling to use (default = simple)
	  * @return A model based on this data, including the specified token string
	  */
	def toModelWith(tokenString: String, style: ModelStyle = Simple) = {
		val base = token.toModelWith(tokenString)
		base ++ (style match {
			case Simple =>
				Vector(Constant("type", tokenType.name), Constant("scopes", scopes.map { _.toSimpleModel }.toVector))
			case Full =>
				Vector(Constant("type", tokenType.toModel), Constant("scopes", scopes.map { _.toModel }.toVector))
		})
	}
}
