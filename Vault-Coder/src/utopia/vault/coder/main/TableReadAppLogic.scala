package utopia.vault.coder.main

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.time.Today
import utopia.flow.util.console.{ArgumentSchema, CommandArguments}
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.reader.TableReader
import utopia.vault.database.Connection

import java.nio.file.Path
import scala.io.{Codec, StdIn}

/**
  * An application logic for generating an input model structure from a database table (or tables)
  * @author Mikko Hilpinen
  * @since 17.10.2022, v1.7.1
  */
object TableReadAppLogic extends AppLogic
{
	implicit val codec: Codec = Codec.UTF8
	
	override val name = "read"
	override lazy val argumentSchema = Vector(
		ArgumentSchema("password", "pw", help = "Password used for accessing the database"),
		ArgumentSchema("user", "u", "root", help = "User name used to access the database"),
		ArgumentSchema("connection", "con", "jdbc:mysql://localhost:3306/", help = "Database address to connect to"),
		ArgumentSchema("database", "db", help = "Name of the read database"),
		ArgumentSchema("table", "t", help = "Name of the read table"),
		ArgumentSchema("output", "out", "output", help = "Directory where the generated file will be placed"),
		ArgumentSchema.flag("all", "A", help = "Whether all tables should be read")
	)
	
	override def apply(args: CommandArguments) = {
		// Specifies the connection settings
		Connection.modifySettings { _.copy(connectionTarget = args("connection").getString,
			user = args("user").getString, password = args("password").getString,
			defaultDBName = args("database").string) }
		// Checks which tables to read
		args("database").string
			.orElse { StdIn.readNonEmptyLine("What's the name of the read database? (empty cancels)") }
			.foreach { dbName =>
				val tableNames: Vector[String] = {
					if (args("all").getBoolean)
						Vector()
					else
						args("table").vector
							.map { _.flatMap { _.string } }
							.filter { _.nonEmpty }
							.getOrElse {
								StdIn.printAndReadLine(
									"Please list the tables you want to read\nSeparate table names with space\nLeave empty to read all tables")
									.split(Regex.whiteSpace).toVector
									.map { _.stripControlCharacters.trim }.filter { _.nonEmpty }
							}
				}
				// Reads the tables
				val models = {
					if (tableNames.isEmpty) {
						println(s"Reading all tables in $dbName")
						TableReader(dbName)
					}
					else {
						if (tableNames.size == 1)
							println(s"Reading $dbName.${ tableNames.head }")
						else
							println(s"Reading [${ tableNames.mkString(", ") }] in $dbName")
						tableNames.map { TableReader(dbName, _) }
					}
				}
				// Writes the output
				val fileName = Today.toLocalDate.toString + (if (tableNames.size == 1) s"-${tableNames.head}" else "") +
					".json"
				((args("output").getString: Path).asExistingDirectory.get/fileName)
					.writeJson(if (models.size == 1) models.head else models).get.openFileLocation()
				
				println(s"${ models.size } class templates generated!")
			}
	}
}
