package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.{Get, Post}
import utopia.access.model.enumeration.Status.BadRequest
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.{ApiNode, LeafNode}
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.Follow
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.access.scope.AccessScopes
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.database.store.ScopeDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.post.NewScope

/**
 * An API node for interacting with authentication scopes
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
class ScopesNode(readScope: ScopeTarget, editScope: ScopeTarget) extends ApiNode[AuthContext[Any] with PostContext]
{
	// ATTRIBUTES   -----------------------
	
	override val name: String = "scopes"
	override val allowedMethods: Iterable[Method] = Pair(Get, Post)
	
	
	// IMPLEMENTED  -----------------------
	
	override def follow(step: String)
	                   (implicit context: AuthContext[Any] with PostContext): PathFollowResult[AuthContext[Any] with PostContext] =
	{
		// Case: Targeting accessible scopes
		if (step ~== "accessible")
			Follow(AccessibleScopesNode)
		// Case: Likely targeting a specific scope
		else
			Follow(new ScopeNode(readScope, editScope, ScopeTarget.forValue(step)))
	}
	
	override def apply(method: Method, remainingPath: Seq[String])
	                  (implicit context: AuthContext[Any] with PostContext): RequestResult =
		method match {
			// Case: POST => Creates one or more new scopes
			case Post =>
				// Requires write access
				context.authorizedFor(editScope) { (_, connection) =>
					// Expects a JSON object or object array body
					context.parseArrayBody(NewScope) { newScopes =>
						// Case: No proper body specified => 400
						if (newScopes.isEmpty)
							BadRequest -> "Please specify the new scope(s) in the request body"
						else {
							// Stores the scope data
							implicit val c: Connection = connection
							ScopeDb.store(newScopes.flatMap { _.links },
								newScopes.iterator.filter { _.grants.isEmpty }.map { _.name }.toOptimizedSeq)
							
							// Returns information about the stored scopes
							RequestResult(AccessScopes.forKeys(newScopes.view.map { _.name }.toSet).pullResponseModels
								.oneOrMany match
							{
								case Left(only) => only.toModel
								case Right(scopes) => scopes
							})
						}
					}
				}
			// Case: GET => Retrieves information about the registered scopes
			case _ =>
				context.authorizedFor(readScope) { (token, connection) =>
					implicit val c: Connection = connection
					RequestResult(AccessScopes.root.root.pullResponseModels.map { scope =>
						scope.toModelWithAccessibleScopeIds(AccessTokenScopes.usable.ofToken(token.id).scopeIds.toSet)
					})
				}
		}
	
	
	// NESTED   --------------------------
	
	private object AccessibleScopesNode extends LeafNode[AuthContext[Any] with PostContext]
	{
		override def name: String = "accessible"
		override def allowedMethods: Iterable[Method] = Single(Get)
		
		override def apply(method: Method, remainingPath: Seq[String])
		                  (implicit context: AuthContext[Any] with PostContext): RequestResult =
			context.authorizedFor(readScope) { (token, connection) =>
				implicit val c: Connection = connection
				RequestResult(AccessScopes.whereTokenLinks.ofToken(token.id).pullResponseModels)
			}
	}
}
