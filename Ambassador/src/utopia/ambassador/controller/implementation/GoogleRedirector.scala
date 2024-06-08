package utopia.ambassador.controller.implementation

import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.citadel.database.access.single.user.DbUser
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.vault.database.Connection

import scala.collection.immutable.VectorBuilder
import scala.io.Codec

object GoogleRedirector
{
	/**
	 * The default redirector implementation for Google cloud services
	 */
	val default = apply()
}

/**
  * A redirector implementation that is compatible with Google cloud services
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class GoogleRedirector(shouldUserSelectAccount: Boolean = false, shouldAlwaysAskForConsent: Boolean = false)
	extends AuthRedirector
{
	// Google expects UTF-8 encoded parameter values
	override def parameterEncoding = Some(Codec.UTF8)
	
	// Could also add prompt (none | consent and/or select_account)
	override def extraParametersFor(settings: AuthServiceSettings, preparation: AuthPreparation, scopes: Seq[Scope])
	                               (implicit connection: Connection) =
	{
		// By default, includes access_type, include_granted_scopes and prompt
		val promptValue = {
			val builder = new VectorBuilder[String]()
			if (shouldAlwaysAskForConsent)
				builder += "consent"
			if (shouldUserSelectAccount)
				builder += "select_account"
			val values = builder.result()
			if (values.isEmpty)
				"none"
			else
				values.mkString(" ")
		}
		val base = Model(Vector(
			"access_type" -> "offline",
			"include_granted_scopes" -> true,
			"prompt" -> promptValue
		))
		// Adds scopes if they are not empty
		val scopesConstant = if (scopes.isEmpty) None else
			Some(Constant("scope", scopes.map { _.name }.mkString(" ")))
		// Reads user email address for login_hint parameter
		val hintConstant = DbUser(preparation.userId).settings.email.map { email => Constant("login_hint", email) }
		
		base ++ scopesConstant ++ hintConstant
	}
}
