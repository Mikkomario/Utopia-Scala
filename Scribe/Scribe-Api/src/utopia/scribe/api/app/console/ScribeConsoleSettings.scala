package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.parse.file.FileExtensions._
import utopia.vault.database.Connection

import java.io.FileNotFoundException
import java.nio.file.{Path, Paths}
import scala.util.Failure

/**
  * An interface for user-specified settings (json-based)
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleSettings
{
	// ATTRIBUTES   ----------------
	
	private lazy val loaded = Paths.get(".").allChildrenIterator
		.find { _.toOption.exists { p => p.fileName.toLowerCase.contains("settings") && p.fileType == "json" } }
		.getOrElse { Failure(new FileNotFoundException("No settings.json file found")) }
		.flatMap { JsonBunny(_).flatMap { v => Settings(v.getModel) } }
	
	
	// COMPUTED --------------------
	
	/**
	  * @return The directory where errors should be logged.
	  *         None if no directory has been specified.
	  */
	def logDirectory = loaded.toOption.flatMap { _.logDirectory }
	
	/**
	  * @return Name of the database to connect to when using Scribe features
	  */
	def dbName = loaded.get.dbName
	
	
	// OTHER    --------------------
	
	/**
	  * Initializes database connection settings
	  * @return Success or failure
	  */
	def initializeDbSettings() = loaded.map { settings =>
		Connection.modifySettings { _.copy(connectionTarget = settings.dbAddress, user = settings.dbUser,
			password = settings.dbPassword, defaultDBName = Some(settings.dbName))
		}
	}
	
	
	// NESTED   --------------------
	
	private object Settings extends FromModelFactoryWithSchema[Settings]
	{
		override lazy val schema: ModelDeclaration = ModelDeclaration(
			PropertyDeclaration("log_directory", StringType, Vector("log"), isOptional = true)
		).withChild("database", ModelDeclaration(
			PropertyDeclaration("address", StringType, Vector("target", "url"), "jdbc:mysql://localhost:3306/"),
			PropertyDeclaration("user", StringType, defaultValue = "root"),
			PropertyDeclaration("password", StringType, Vector("pw"), isOptional = true),
			PropertyDeclaration("name", StringType, defaultValue = "scribe_db")
		))
		
		override protected def fromValidatedModel(model: Model): Settings = {
			val db = model("database").getModel
			apply(db("address").getString, db("user").getString, db("password").getString, db("name").getString,
				model("log_directory").string.flatMap { s => (s: Path).asExistingDirectory.toOption })
		}
	}
	private case class Settings(dbAddress: String, dbUser: String, dbPassword: String, dbName: String,
	                            logDirectory: Option[Path])
}
