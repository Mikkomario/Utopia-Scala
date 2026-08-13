package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.{BadRequest, Forbidden, NotFound}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.util.StringExtensions._
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.LeafNode
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.access.scope.AccessScopes
import utopia.vigil.database.access.token.template.AccessTokenTemplates
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.combined.token.DetailedTokenTemplate
import utopia.vigil.model.post.NewTokenTemplate

/**
 * An API node for creating new token templates.
 * Intended for developer access only. Intended to appear as /tokens/templates.
 * @author Mikko Hilpinen
 * @since 22.05.2026, v0.1
 */
class TokenTemplatesNode(requiredScope: ScopeTarget) extends LeafNode[AuthContext[Any] with PostContext]
{
	// ATTRIBUTES   -------------------------
	
	override val name: String = "templates"
	override val allowedMethods: Iterable[Method] = Single(Post)
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(method: Method, remainingPath: Seq[String])
	                  (implicit context: AuthContext[Any] with PostContext): RequestResult =
		context.authorizedFor(requiredScope) { (_, connection) =>
			context.parseBody(NewTokenTemplate) { newTemplate =>
				implicit val c: Connection = connection
				
				// Makes sure the template name is unique
				val newTemplateNames = newTemplate.newTemplateNames
				if (newTemplateNames.isDistinct)
					AccessTokenTemplates.withNames(newTemplateNames).names.pull.notEmpty match {
						// Case: Not unique => 403
						case Some(duplicateNames) =>
							Forbidden -> s"The following token templates already exist: [${
								duplicateNames.iterator.map { _.quoted }.mkString(", ") }]"
							
						// Case: Unique => Resolves the referenced scopes, if possible
						case None =>
							val scopeReferences = newTemplate.referencedScopes
							val scopeByKey = AccessScopes.forKeys(scopeReferences).toMapBy { _.key.toLowerCase }
							scopeReferences.iterator.filterNot { key => scopeByKey.contains(key.toLowerCase) }
								.notEmpty match
							{
								// Case: No all scopes could be resolved => 404
								case Some(missingScopesIter) =>
									NotFound -> s"The following referenced scopes have not been registered: [${
										missingScopesIter.map { _.quoted }.mkString(", ") }]"
								
								// Case: Scopes are valid => Resolves the referenced token templates
								case None =>
									val templateReferences = newTemplate.referencedTemplateNames
									val templateIdByName = AccessTokenTemplates.withNames(templateReferences).idByName
									templateReferences.iterator
										.filterNot { name => templateIdByName.contains(name.toLowerCase) }
										.notEmpty match
									{
										// Case: Not all template references can be resolved => 404
										case Some(missingTemplatesIter) =>
											NotFound ->
												s"The following referenced token templates have not been registered: [${
													missingTemplatesIter.map { _.quoted }.mkString(", ") }]"
											
										// Case: Input is valid => Stores the new token template(s)
										case None =>
											RequestResult(storeTemplate(newTemplate, scopeByKey, templateIdByName))
									}
							}
					}
				// Case: Not unique within input => 400
				else
					BadRequest -> "Template names must be unique"
			}
		}
		
	
	// OTHER    ---------------------------
	
	private def storeTemplate(template: NewTokenTemplate, scopeByKey: Map[String, ScopeTarget],
	                          templateIdByName: Map[String, Int])
	                         (implicit connection: Connection): DetailedTokenTemplate =
	{
		val grantedTemplateIds = template.grants.map {
			case Left(newTemplate) => storeTemplate(newTemplate, scopeByKey, templateIdByName).id
			case Right(templateName) => templateIdByName(templateName.toLowerCase)
		}
		TokenDb.createTemplate(template.name, template.scopeGrantType,
			template.accessibleScopes.map { key => scopeByKey(key.toLowerCase) },
			template.forwardedScopes.map { key => scopeByKey(key.toLowerCase) }, grantedTemplateIds, template.duration,
			template.revokesOriginal, template.revokesEarlier)
	}
}