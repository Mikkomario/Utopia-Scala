package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.ScopeData
import utopia.exodus.model.stored.auth.{Scope, TokenScopeLink}
import utopia.flow.collection.value.typeless
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.view.template.Extender
import utopia.metropolis.model.StyledModelConvertible

/**
  * An access scope that is tied to a specific access token
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
case class TokenScope(scope: Scope, tokenLink: TokenScopeLink) extends Extender[ScopeData] with StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this scope in the database
	  */
	def id = scope.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = scope.data
	
	override def toModel = scope.toModel + immutable.Constant("token_link", tokenLink.toModel)
	
	override def toSimpleModel = scope.toSimpleModel ++ Vector(
		immutable.Constant("is_directly_accessible", tokenLink.isDirectlyAccessible),
		immutable.Constant("grants_forward", tokenLink.grantsForward))
}

