package utopia.vigil.controller.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.NotEmpty
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender
import utopia.vigil.database.VigilContext._
import utopia.vigil.database.access.token.AccessTokens
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.cached.scope.ScopeTarget

import scala.io.StdIn

/**
 * Provides console commands for managing the Vigil authentication system
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
object VigilCommands extends Extender[Seq[Command]]
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * Sets up the developer API key.
	 * Only included while no authentication tokens have been registered.
	 */
	private lazy val setup = connectionPool
		.logging { implicit c =>
			if (AccessTokens.active.isEmpty)
				Some(Command("setup", help = "Sets up a developer API key for the Vigil authorization system")(
					ArgumentSchema("scope", help = "Scope given to the developer API key",
						defaultDescription = "Requested"),
					ArgumentSchema("scopes",
						help = "Other registered scopes. All these are placed under `scope`.\nValues can be chained using `>`, so that they form hierarchies. E.g. `[admin>general.write>general.read,specific.read>general.read]`.",
						defaultDescription = "Requested")) {
						args =>
							lazy val scope = args("scope").stringOr {
								StdIn.readNonEmptyLine(
										"Which scope should be granted to this token? Default = \"developer\".")
									.getOrElse("developer")
							}
							TokenDb.setupDeveloperKeyIfNeeded(ScopeTarget(scope),
								NotEmpty(args("scopes").getVector)
									.getOrElse {
										StdIn.readNonEmptyLine(
											s"Which scopes should be placed below the `$scope` scope?\n\nNB: You can chain scopes using `>`, so that they form hierarchies. \nE.g. [admin > general.write > general.read, specific.read > general.read]") match
										{
											case Some(input) => input.getVector
											case None => Empty
										}
									}
									.flatMap { value =>
										value.getString.splitIterator(Regex.escape('>')).map { _.trim }.pairedFrom(scope)
									}) match
							{
								case Some((key, _, _)) =>
									println("Developer token created. \nPlease store it in a secure and private location; \nThis token CANNOT BE RECOVERED if forgotten or lost.")
									println(key)
									
								case None =>
									println("Authentication tokens had already been set up, so no new developer key was created.")
							}
					})
			else
				None
		}
		.flatten
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped: Seq[Command] = setup.emptyOrSingle
}
