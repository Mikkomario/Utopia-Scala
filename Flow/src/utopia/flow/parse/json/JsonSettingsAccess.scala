package utopia.flow.parse.json

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Constant, Model, ModelDeclaration, Value}
import utopia.flow.generic.model.template.{HasPropertiesLike, PropertiesWrapper}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.FileUtils
import utopia.flow.parse.json.JsonSettingsAccess.defaultSettingsRegex
import utopia.flow.parse.string.Regex
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._

import java.io.FileNotFoundException
import java.nio.file.Path
import scala.util.{Failure, Try}

object JsonSettingsAccess
{
	/**
	 * A regular expression that finds a file that contains "settings" or "Settings" in its file name
	 */
	val defaultSettingsRegex = Regex.any + (Regex("s") || Regex("S")).withinParentheses + Regex("ettings") + Regex.any
}

/**
 * An interface used for accessing settings from a JSON file
 * @author Mikko Hilpinen
 * @since 14.9.2023, v2.2
 *
 * @constructor Creates a new settings access interface
 * @param rootDirectory Directory from which settings files are scanned. May also be a settings .json file itself.
 *                      Default = current working directory.
 * @param fileNameRegex A regular expression that must be fulfilled by the targeted settings file's file name -part.
 *                      By default, accepts any file that contains "settings" or "Settings" in its name.
 *                      Note: This file name -part doesn't include the file type extension,
 *                      which is required to match "json".
 * @param schema        A model declaration that must be fulfilled by the JSON object contents of the targeted file.
 *                      By default, accepts any JSON object.
 * @param jsonReader    A JSON parser used to parse the settings JSON file(s)
 * @param log           A logging implementation notified in case settings reading fails
 *                      (e.g. if no settings file may be found or if JSON parsing fails)
 */
class JsonSettingsAccess(rootDirectory: Path = FileUtils.workingDirectory, fileNameRegex: Regex = defaultSettingsRegex,
                         schema: ModelDeclaration = ModelDeclaration.empty)
                        (implicit jsonReader: JsonParser, log: Logger)
	extends PropertiesWrapper[Constant]
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * The model that was parsed from a settings file.
	 * Contains a failure if no settings file was found, one couldn't be parsed or if the file contents
	 * didn't fulfill all the schema requirements.
	 */
	val toModel = {
		// Case: Invalid root directory => Fails
		if (rootDirectory.notExists)
			Failure(new FileNotFoundException(s"The specified root directory ${rootDirectory.absolute} doesn't exist"))
		else {
			val targetFilesIterator = rootDirectory.toTree.topDownNodesIterator.map { _.nav }.filter { p =>
				val (fileName, fileType) = p.fileNameAndType.toTuple
				fileType.equalsIgnoreCase("json") && fileNameRegex(fileName)
			}
			// Case: No suitable file found => Fails
			if (!targetFilesIterator.hasNext)
				Failure(new FileNotFoundException(
					s"${rootDirectory.absolute} doesn't contain a suitable settings file"))
			// Case: Suitable files available =>
			// Finds the first file that can be successfully parsed and contains the required properties
			else
				targetFilesIterator.map { jsonReader(_).flatMap { m => schema.validate(m.getModel) } }
					.trySucceedOnce.toTry
		}
	}
	/**
	 * The model that was read from a settings file.
	 * An empty model in case of a parse failure.
	 */
	// Failures are logged. An empty model is used in case of a failure.
	protected lazy val model = toModel.logWithMessage("Failed to access settings").getOrElse(Model.empty)
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether these settings are accessible. I.e. whether they were successfully read.
	 */
	def isAccessible = toModel.isSuccess
	@deprecated("Renamed to isAccessible", "v2.8")
	def accessible = isAccessible
	/**
	 * @return Whether these settings are not accessible. I.e. the reading process failed.
	 */
	def nonAccessible = !isAccessible
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def wrapped: HasPropertiesLike[Constant] = model
	
	override def tryGet[A](propName: String, altPropNames: String*)(f: Value => Try[A]): Try[A] =
		tryGet(propName +: altPropNames)(f)
	override def tryGet[A](propNames: Iterable[String])(f: Value => Try[A]): Try[A] =
		toModel.flatMap { _.tryGet(propNames)(f) }
	
	
	// OTHER    -------------------------
	
	/**
	 * @param settingName Name of the targeted setting / property
	 * @return Non-empty value of the targeted property. Failure if no value is defined or if settings-parsing failed.
	 */
	@deprecated("Please use .tryGet(...) instead", "v2.8")
	def required(settingName: String) = toModel.flatMap { m =>
		m.nonEmpty(settingName).toTry { new NoSuchElementException(s"Required setting $settingName is missing") }
	}
}
