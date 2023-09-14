package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.JsonSettingsAccess.{defaultSettingsRegex, splitterRegex}
import utopia.flow.util.logging.Logger

import java.io.FileNotFoundException
import java.nio.file.Path
import scala.util.Failure

object JsonSettingsAccess
{
	/**
	 * A regular expression that finds a file that contains "settings" or "Settings" in its file name
	 */
	val defaultSettingsRegex = Regex.any + (Regex("s") || Regex("S")).withinParenthesis + Regex("ettings") + Regex.any
	
	private val splitterRegex = Regex.escape('/')
}

/**
 * An interface used for accessing settings from a json file
 * @author Mikko Hilpinen
 * @since 14.9.2023, v2.2
 *
 * @constructor Creates a new settings access interface
 * @param rootDirectory Directory from which settings files are scanned. May also be a settings .json file itself.
 *                      Default = current working directory.
 * @param fileNameRegex A regular expression that must be fulfilled by the targeted settings file's file name -part.
 *                      By default, accepts any file that contains "settings" or "Settings" in its name.
 * @param schema        A model declaration that must be fulfilled by the json object contents of the targeted file.
 *                      By default, accepts any json object.
 * @param jsonReader    A json parser used to parse the settings json file(s)
 * @param log           A logging implementation notified in case settings reading fails
 *                      (e.g. if no settings file may be found or if json parsing fails)
 */
class JsonSettingsAccess(rootDirectory: Path = "", fileNameRegex: Regex = defaultSettingsRegex,
                         schema: ModelDeclaration = ModelDeclaration.empty)
                        (implicit jsonReader: JsonParser, log: Logger)
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * The model that was parsed from a settings file.
	 * Contains a failure if no settings file was found, one couldn't be parsed or if the file contents
	 * didn't fulfill all the schema requirements.
	 */
	protected lazy val parsed = {
		// Case: Invalid root directory => Fails
		if (rootDirectory.notExists)
			Failure(new FileNotFoundException(s"The specified root directory ${rootDirectory.absolute} doesn't exist"))
		else {
			val targetFilesIterator = rootDirectory.toTree.topDownNodesIterator.map { _.nav }.filter { p =>
				val (fileName, fileType) = p.fileNameAndType.toTuple
				fileType.equalsIgnoreCase("json") && fileNameRegex(fileName)
			}
			// Case: No suitable file found => Fails
			if (targetFilesIterator.hasNext)
				Failure(new FileNotFoundException(
					s"${rootDirectory.absolute} doesn't contain a suitable settings file"))
			// Case: Suitable files available =>
			// Finds the first file that can be successfully parsed and contains the required properties
			else
				targetFilesIterator.map { jsonReader(_).flatMap { m => schema.validate(m.getModel).toTry } }
					.trySucceedOnce.toTry
		}
	}
	/**
	 * The model that was read from a settings file.
	 * An empty model in case of a parse failure.
	 */
	// Failures are logged. An empty model is used in case of a failure.
	protected lazy val model = parsed.getOrElseLog(Model.empty)
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether these settings are accessible. I.e. whether they were successfully read.
	 */
	def accessible = parsed.isSuccess
	/**
	 * @return Whether these settings are not accessible. I.e. the reading process failed.
	 */
	def nonAccessible = !accessible
	
	
	// OTHER    -------------------------
	
	/**
	 * @param settingName Name of the targeted setting property
	 * @return A value that matches that property
	 */
	def apply(settingName: String) = model(settingName)
	/**
	 * @param prio1Key Priority 1 setting property name
	 * @param prio2Key Alternative name for that property
	 * @param moreKeys More alternative names
	 * @return A value that matches any of those properties
	 */
	def apply(prio1Key: String, prio2Key: String, moreKeys: String*) =
		model(Pair(prio1Key, prio2Key) ++ moreKeys)
	
	/**
	 * @param settingName Name of the targeted setting / property
	 * @return Non-empty value of the targeted property. Failure if no value is defined or if settings-parsing failed.
	 */
	def required(settingName: String) = parsed.flatMap { m =>
		m.nonEmpty(settingName).toTry { new NoSuchElementException(s"Required setting $settingName is missing") }
	}
}
