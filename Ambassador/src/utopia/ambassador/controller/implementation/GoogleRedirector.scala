package utopia.ambassador.controller.implementation

import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.citadel.database.access.single.DbUser
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection

import scala.io.Codec

/**
  * A redirector implementation that is compatible with Google cloud services
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object GoogleRedirector extends AuthRedirector
{
	// Google expects UTF-8 encoded parameter values
	override def parameterEncoding = Some(Codec.UTF8)
	
	// Could also add prompt (none | consent and/or select_account)
	override def extraParametersFor(settings: ServiceSettings, preparation: AuthPreparation, scopes: Vector[Scope])
	                               (implicit connection: Connection) =
	{
		// By default, includes access_type and include_granted_scopes
		val base = Model(Vector(
			"access_type" -> "offline",
			"include_granted_scopes" -> true
		))
		// Adds scopes if they are not empty
		val scopesConstant = if (scopes.isEmpty) None else
			Some(Constant("scope", scopes.map { _.officialName }.mkString(" ")))
		// Reads user email address for login_hint parameter
		val hintConstant = DbUser(preparation.userId).settings.email.map { email => Constant("login_hint", email) }
		
		base ++ scopesConstant ++ hintConstant
	}
}
