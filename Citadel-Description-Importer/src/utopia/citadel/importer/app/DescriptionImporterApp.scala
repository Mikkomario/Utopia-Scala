package utopia.citadel.importer.app

import utopia.bunnymunch.jawn.JsonBunny
import utopia.citadel.importer.controller.ReadDescriptions
import utopia.citadel.util.CitadelContext
import utopia.flow.async.context.ThreadPool
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.console.{ArgumentSchema, CommandArguments, CommandArgumentsSchema}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.vault.database.{Connection, ConnectionPool}

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * An application for importing Metropolis-style descriptions from a json file
  * @author Mikko Hilpinen
  * @since 26.6.2021, v1.0
  */
object DescriptionImporterApp extends App
{
	DataType.setup()
	
	implicit val logger: Logger = SysErrLogger
	implicit val jsonParser: JsonParser = JsonBunny
	implicit val executionContext: ExecutionContext = new ThreadPool("Description-Importer").executionContext
	val connectionPool = new ConnectionPool()
	
	// Sets up context based on settings
	val commandSchema = CommandArgumentsSchema(Vector(
		ArgumentSchema("input", "in"),
		ArgumentSchema("database", "db"),
		ArgumentSchema("user", "u"),
		ArgumentSchema("password", "pw"),
		ArgumentSchema("settings", "read", "description-importer-settings.json")
	))
	val arguments = CommandArguments(commandSchema, args.toVector)
	
	val settingsPath: Path = arguments("settings").getString
	lazy val settings = jsonParser(settingsPath) match
	{
		case Success(value) => value.getModel
		case Failure(_) =>
			println(s"Warning: Not reading external settings")
			Model.empty
	}
	
	// Makes sure the input path exists
	arguments("input").string.orElse { settings("input").string } match
	{
		case Some(inputPathStr) =>
			val inputPath: Path = inputPathStr
			if (inputPath.notExists)
				println(s"The input path you have specified (${inputPath.toAbsolutePath}) doesn't exist")
			else
			{
				// Specifies the rest of the settings
				val databaseName = arguments("database").stringOr { settings("database").stringOr("exodus_db") }
				println(s"Using database: $databaseName")
				CitadelContext.setup(executionContext, connectionPool, databaseName)
				Connection.modifySettings { s => s.copy(
					user = arguments("user").stringOr { settings("user").stringOr(s.user) },
					password = arguments("password").stringOr { settings("password").getString })
				}
				
				// Attempts to read the target
				val result = {
					if (inputPath.isDirectory)
					{
						inputPath.allRegularFileChildrenOfType("json").flatMap { filePaths =>
							if (filePaths.isEmpty)
							{
								println(s"Specified input directory (${
									inputPath.toAbsolutePath}) doesn't contain any .json files to read")
								Success(())
							}
							else
							{
								println(s"Reading ${filePaths.size} files in ${inputPath.toAbsolutePath}")
								connectionPool.tryWith { implicit connection =>
									filePaths.tryForeach { path =>
										val result = ReadDescriptions(path)
										if (result.isSuccess)
											println(s"Successfully read ${inputPath.fileName}")
										else
											println(s"Failed to read ${inputPath.fileName}")
										result
									}
								}.flatten
							}
						}
					}
					else
					{
						println(s"Reading ${inputPath.toAbsolutePath}...")
						connectionPool.tryWith { implicit c => ReadDescriptions(inputPath) }.flatten
					}
				}
				result match
				{
					case Success(_) => println("Descriptions import successfully completed")
					case Failure(error) => error.printStackTrace()
				}
			}
		case None =>
			println(s"Please specify the target file or directory by specifying parameter 'input' " +
				s"either as a command line argument or in a separate settings json file (currently: '${
					settingsPath.toAbsolutePath}')")
	}
}
