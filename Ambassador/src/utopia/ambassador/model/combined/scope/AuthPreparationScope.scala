package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.util.Extender

/**
  * Combines Scope with preparationLink data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationScope(scope: Scope, preparationLink: AuthPreparationScopeLink) 
	extends Extender[ScopeData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this Scope in the database
	  */
	def id = scope.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = scope.data
}

