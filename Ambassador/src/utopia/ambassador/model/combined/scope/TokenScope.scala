package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.util.Extender

/**
  * Attaches a token link to a scope
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class TokenScope(scope: Scope, tokenId: Int, linkId: Int) extends Extender[ScopeData]
{
	def id = scope.id
	
	override def wrapped = scope
}
