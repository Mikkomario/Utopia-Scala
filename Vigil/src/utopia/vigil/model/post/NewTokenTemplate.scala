package utopia.vigil.model.post

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.time.Duration
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.UncertainBoolean
import utopia.vigil.model.enumeration.ScopeGrantType
import utopia.vigil.model.enumeration.ScopeGrantType.{Copy, Dictate}

import scala.util.{Success, Try}

object NewTokenTemplate extends FromModelFactory[NewTokenTemplate]
{
	// IMPLEMENTED  ---------------------------
	
	// Parses the scopes
	override def apply(model: HasProperties): Try[NewTokenTemplate] =
		model.tryGet("scopes", "directScopes", "scopes_direct") { _.tryVectorWith { _.tryString } }
			.flatMap { accessibleScopes =>
				model.tryGet("forwardedScopes", "scopes_forwarded") { _.tryVectorWith { _.tryString } }
					.flatMap { forwardedScopes =>
						// Determines the grant type
						// The default is determined by whether scopes have been specified
						val grantType = model.tryGet("grantType", "grant_type") {
							_.notEmpty match {
								case Some(grantTypeValue) => ScopeGrantType.fromValue(grantTypeValue)
								case None =>
									Success(if (accessibleScopes.isEmpty && forwardedScopes.isEmpty) Copy else Dictate)
							}
						}
						grantType.flatMap { grantType =>
							// Parses token duration. Default = Infinite.
							val duration = model.tryGet("duration", "expiresIn", "expires_in") {
								_.notEmpty match {
									case Some(value) => value.tryDuration
									case None => Success(Duration.infinite)
								}
							}
							duration.flatMap { duration =>
								// Parses the granted token types
								model
									.tryGet("grants") { _.tryVectorWith {
										_.getModelOrString match {
											case Left(newTokenModel) => apply(newTokenModel).map { Left(_) }
											case Right(tokenTemplateName) => Success(Right(tokenTemplateName))
										}
									} }
									.map { grantedTokenTypes =>
										apply(model("name").getString, grantType, accessibleScopes, forwardedScopes,
											grantedTokenTypes, duration,
											model("swaps", "singleUse", "single_use",
												"revokesOriginal", "revokes_original").getBoolean,
											model("revokesEarlier", "revokes_earlier").boolean)
									}
							}
						}
					}
			}
}

/**
 * A POST model for creating a new token template
 * @param name Name of this template
 * @param scopeGrantType Method of scope-granting applied
 * @param accessibleScopes Scopes (keys) directly accessible with this token type
 * @param forwardedScopes Scopes (keys) forwarded to the granted token(s)
 * @param grants Granted scope types, where values are either:
 *               - Left: New token templates
 *               - Right: Names of existing token templates
 * @param duration Maximum duration of generated tokens
 * @param revokesOriginal Whether this token is revoked when a new token is constructed using it
 * @param revokesEarlier Whether previously generated tokens are to be revoked
 *                       when a new token is constructed using this kind of token.
 *                       Uncertain if determined by the caller.
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
case class NewTokenTemplate(name: String, scopeGrantType: ScopeGrantType, accessibleScopes: Seq[String],
                            forwardedScopes: Seq[String], grants: Seq[Either[NewTokenTemplate, String]],
                            duration: Duration, revokesOriginal: Boolean, revokesEarlier: UncertainBoolean)
{
	/**
	 * @return Names of these new templates
	 */
	def newTemplateNames: Seq[String] = Single(name).filter { _.nonEmpty } ++
		nestedTemplatesIter.flatMap { _.newTemplateNames }
	
	/**
	 * @return Scope keys referenced by this token template
	 */
	def referencedScopes: Set[String] =
		(accessibleScopes.iterator ++ forwardedScopes.iterator ++ nestedTemplatesIter.flatMap { _.referencedScopes })
			.toSet
	/**
	 * @return Names of the token templates referenced by this token template
	 */
	def referencedTemplateNames: Set[String] = grants.iterator
		.flatMap {
			case Left(newTemplate) => newTemplate.referencedTemplateNames
			case Right(templateName) => Single(templateName)
		}
		.toSet
	
	private def nestedTemplatesIter = grants.iterator.flatMap { _.leftOption }
}