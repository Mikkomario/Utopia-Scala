package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.{Delete, Get, Post, Put}
import utopia.access.model.enumeration.Status.{BadRequest, NotFound}
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.{ApiNode, LeafNode, NodeWithChildren}
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.access.scope.AccessScopes
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.database.storable.scope.ScopeDbModel
import utopia.vigil.database.store.ScopeDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.partial.scope.ScopeData
import utopia.vigil.model.stored.scope.Scope

/**
 * An API node that represents a single scope
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
class ScopeNode(readScope: ScopeTarget, editScope: ScopeTarget, target: ScopeTarget)
	extends NodeWithChildren[AuthContext[Any] with PostContext]
{
	// IMPLEMENTED  ---------------------
	
	override def name: String = target.toString
	override def children: Iterable[ApiNode[AuthContext[Any] with PostContext]] = Single(GrantsNode)
	override def allowedMethods: Iterable[Method] = Set(Get, Post, Put, Delete)
	
	override def apply(method: Method, remainingPath: Seq[String])
	                  (implicit context: AuthContext[Any] with PostContext): RequestResult =
		context.authorizedFor(if (method == Get) readScope else editScope) { (token, connection) =>
			implicit val c: Connection = connection
			method match {
				// Case: POST => Posts a new scope, if possible
				case Post =>
					val name = target.key
					if (name.isEmpty)
						BadRequest -> s"Invalid scope $target"
					else if (name.length > Scope.maxKeyLength)
						BadRequest -> s"Maximum scope name length is ${ Scope.maxKeyLength }"
					else if (target.isValid)
						RequestResult(target.id)
					else {
						val newScope = ScopeDbModel.insert(ScopeData(name))
						ScopeTarget.update()
						RequestResult(newScope.id)
					}
				
				// Case: PUT => Changes this scope's name
				case Put =>
					context.withBody { body =>
						val newName = body.getString
						if (newName.isEmpty)
							BadRequest -> "Request body must not be empty"
						else if (newName.length > Scope.maxKeyLength)
							BadRequest -> s"Maximum scope name length is ${ Scope.maxKeyLength }"
						else {
							target.access.key.set(newName)
							ScopeTarget.update()
							RequestResult.Empty
						}
					}
				// Case: DELETE => Deletes this scope
				case Delete =>
					target.access.delete()
					ScopeTarget.update()
					RequestResult.Empty
				// Case: GET => Returns this scope's information
				case _ =>
					target.access.pullResponseModel match {
						case Some(response) =>
							// Checks whether this scope is accessible using the current token
							RequestResult(response.toModelWithAccessibleScopeIds(
								AccessTokenScopes.usable.ofToken(token.id).scopeIds.toSet))
						case None => NotFound -> s"$target is not a registered scope"
					}
			}
		}
	
	
	// NESTED   -------------------------
	
	private object GrantsNode extends LeafNode[AuthContext[Any] with PostContext]
	{
		override def name: String = "grants"
		override def allowedMethods: Iterable[Method] = Set(Get, Put, Post, Delete)
		
		override def apply(method: Method, remainingPath: Seq[String])
		                  (implicit context: AuthContext[Any] with PostContext): RequestResult =
		{
			// Requires read or write -authentication, based on the method used
			context.authorizedFor(if (method == Get) readScope else editScope) { (_, connection) =>
				// Case: Valid token => Parses the request body; Expects scope keys.
				if (target.isValid)
					context.withArrayBody { values =>
						implicit val c: Connection = connection
						val scopeKeys = values.iterator.flatMap { _.string }.toSet
						
						// Executes the changes, if appropriate
						method match {
							// Case: PUT => Overwrites granted scopes
							case Put =>
								if (scopeKeys.isEmpty)
									ScopeDb.deleteGrantsOf(target.id)
								else
									ScopeDb.setGrantsOf(target.id, scopeKeys)
									
								ScopeTarget.update()
							
							// Case: POST => Adds new grants
							case Post =>
								if (scopeKeys.nonEmpty) {
									ScopeDb.store(scopeKeys.map { child => Pair(target.key, child) })
									ScopeTarget.update()
								}
								
							// Case: DELETE => Removes grants
							case Delete =>
								if (scopeKeys.isEmpty) {
									if (context.lazyParsedRequestBody.value.exists { _.isEmpty }) {
										ScopeDb.deleteGrantsOf(target.id)
										ScopeTarget.update()
									}
								}
								else {
									ScopeDb.deleteGrantsOf(target.id, scopeKeys)
									ScopeTarget.update()
								}
								
							// Case: GET => No change
							case _ => ()
						}
						
						// Returns the grants after changes
						RequestResult(AccessScopes.whereParentLinks.withParent(target.id).pullResponseModels)
					}
				// Case: Invalid token => 404
				else
					NotFound -> s"$target is not a registered scope"
			}
		}
	}
}
