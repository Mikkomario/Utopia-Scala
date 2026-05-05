package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Delete
import utopia.access.model.enumeration.Status.{Forbidden, Unauthorized}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.nexus.controller.api.node.{ApiNode, LeafNode, NodeWithChildren}
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.access.token.template.{AccessTokenTemplate, AccessTokenTemplates}
import utopia.vigil.database.access.token.{AccessToken, AccessTokens}

import scala.annotation.tailrec

/**
 * An API node used for interacting with (i.e. revoking) individual tokens
 * @author Mikko Hilpinen
 * @since 05.05.2026, v0.1
 */
class TokenNode(targetId: Option[Int]) extends NodeWithChildren[AuthContext[Any]]
{
	// ATTRIBUTES   -----------------
	
	override val allowedMethods: Iterable[Method] = Single(Delete)
	
	
	// COMPUTED ---------------------
	
	override def children: Iterable[ApiNode[AuthContext[Any]]] = Single(new ChildrenNode)
	
	
	// IMPLEMENTED  -----------------
	
	override def name: String = targetId match {
		case Some(targetId) => targetId.toString
		case None => "current"
	}
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: AuthContext[Any]): RequestResult =
		context.authorized { (token, connection) =>
			implicit val c: Connection = connection
			targetId.filterNot { _ == token.id } match {
				// Case: Targeting a specific token
				case Some(targetTokenId) => tryRevokeSpecific(targetTokenId, token.id)
				// Case: Targeting the current token
				case None => tryRevokeCurrent(token.id)
			}
		}
	
	
	// OTHER    ---------------------
	
	private def tryRevokeCurrent(tokenId: Int)(implicit connection: Connection): RequestResult = {
		// Makes sure this token can self-revoke
		if (AccessTokenTemplate.ofToken(tokenId).canRevokeSelf.pull.getOrElse(false)) {
			val access = AccessToken(tokenId)
			// Revokes this token and the child tokens, unless already revoked
			if (access.active.nonEmpty) {
				access.revoke()
				revokeChildrenOfTokens(Single(tokenId))
			}
			RequestResult.Empty
		}
		// Case: Can't self-revoke => 403
		else
			Forbidden -> "This token can't self-revoke"
	}
	
	private def tryRevokeSpecific(tokenId: Int, authorizingTokenId: Int)(implicit connection: Connection): RequestResult = {
		val access = AccessToken(tokenId)
		// Makes sure this request is properly authorized
		if (access.parentIdsIterator.contains(authorizingTokenId)) {
			// Checks whether this token can be revoked from above
			if (AccessTokenTemplate.ofToken(tokenId).parentCanRevoke.pull.getOrElse(false)) {
				// Revokes this token and the child tokens, unless already revoked
				if (access.active.nonEmpty) {
					access.revoke()
					revokeChildrenOfTokens(Single(tokenId))
				}
				RequestResult.Empty
			}
			// Case: Revoking not supported => 403
			else
				Forbidden -> "This token can't be revoked in this authentication scope"
		}
		// Case: Targeting some unrelated token => 401
		else
			Unauthorized -> "This token can't be revoked using the current access token"
	}
	
	// Assumes non-empty input
	@tailrec
	private def revokeChildrenOfTokens(tokenIds: Iterable[Int])(implicit connection: Connection): Unit = {
		// Finds the targeted tokens
		val targetAccess = AccessTokens.active.createdUsingTokens(tokenIds).whereTemplate.revokableByParents
		val nextLayerIds = targetAccess.ids.pull
		
		// Case: More tokens to revoke => Revokes them and continues recursively downwards
		if (nextLayerIds.nonEmpty) {
			if (nextLayerIds.hasSize > tokenIds)
				targetAccess.revoke()
			else
				AccessTokens(nextLayerIds).revoke()
			
			revokeChildrenOfTokens(nextLayerIds)
		}
	}
	
	
	// NESTED   ----------------------------
	
	private class ChildrenNode extends LeafNode[AuthContext[_]]
	{
		// ATTRIBUTES   --------------------
		
		override val name: String = "children"
		override val allowedMethods: Iterable[Method] = Single(Delete)
		
		
		// IMPLEMENTED  --------------------
		
		override def apply(method: Method, remainingPath: Seq[String])
		                  (implicit context: AuthContext[_]): RequestResult =
			context.authorized { (token, connection) =>
				implicit val c: Connection = connection
				targetId.filterNot { _ == token.id } match {
					// Case: Targeting the children of a specific token => Makes sure this request is authorized
					case Some(targetTokenId) =>
						// Case: Authorized => Proceeds to revoke the child tokens, if possible
						if (AccessToken(targetTokenId).parentIdsIterator.contains(token.id))
							tryRevokeChildrenOf(targetTokenId)
						// Case: Unrelated token => 401
						else
							Unauthorized -> "This token can't be revoked using the current access token"
					
					// Case: Targeting the current token => Proceeds to revoke, if possible
					case None => tryRevokeChildrenOf(token.id)
				}
			}
			
		
		// OTHER    ------------------------
		
		private def tryRevokeChildrenOf(tokenId: Int)(implicit connection: Connection): RequestResult = {
			val children = AccessTokens.active.createdUsing(tokenId).idsAndTemplateIds
			// Case: No active child tokens are present => 204
			if (children.isEmpty)
				RequestResult.Empty
			else {
				// Check which of these tokens may be revoked from above
				val canRevokeTemplateIds = AccessTokenTemplates(children.iterator.map { _.second }.toSet).ids.toSet
				val childIdsToRevoke = children.iterator.filter { ids => canRevokeTemplateIds.contains(ids.second) }
					.map { _.first }.toOptimizedSeq
				
				// Case: None of these tokens may be revoked => 403
				if (childIdsToRevoke.isEmpty)
					Forbidden -> "These tokens can't be revoked using the current authentication scope"
				else {
					// Proceeds to revoke these tokens and their child tokens
					AccessTokens(childIdsToRevoke).revoke()
					revokeChildrenOfTokens(childIdsToRevoke)
					RequestResult.Empty
				}
			}
		}
	}
}
