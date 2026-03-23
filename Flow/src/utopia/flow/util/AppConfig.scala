package utopia.flow.util

import utopia.flow.async.context.AccessQueue
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.PropertiesWrapper
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.FileUtils
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.parse.StreamExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryCatch
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

object AppConfig
{
	/**
	 * Creates a new empty app config state
	 * @param path A path to which the config state will be stored, if modified.
	 *             Notice that any existing state in that path will be overwritten.
	 * @param exc Implicit execution context. Used in asynchronous config-saving.
	 * @param log Implicit logging implementation
	 * @return A new empty app config state
	 */
	def empty(path: Path)(implicit exc: ExecutionContext, log: Logger) = new AppConfig(path, Model.empty)
	
	/**
	 * @param appName Name of the targeted application
	 * @param fileName Name of the config file used (default = config.json)
	 * @param allowWorkingDirectoryAsAppDirectory Whether to allow the use of the current working directory
	 *                                            as the app directory in cases where no app directory could be
	 *                                            created or accessed under the user's home directory.
	 * @param jsonParser Implicit JSON parser used for reading the initial config state
	 * @param exc Implicit execution context. Used in asynchronous config-saving.
	 * @param log Implicit logging implementation
	 * @return A new app config instance.
	 *         Failure if the config file couldn't be accessed or created, or if JSON parsing failed.
	 *         Yields partial failures, if failed to properly restrict the file permissions upon file-creation.
	 */
	def apply(appName: => String, fileName: => String = "config.json",
	          allowWorkingDirectoryAsAppDirectory: Boolean = false)
	         (implicit jsonParser: JsonParser, exc: ExecutionContext, log: Logger): TryCatch[AppConfig] =
		FileUtils.appConfigFile(appName, fileName, allowWorkingDirectoryAsAppDirectory).flatMap { apply(_).toTryCatch }
	
	/**
	 * @param path Path which stores the current app configuration
	 * @param allowCreation Whether to allow a new empty config state / file to be created, if 'path' doesn't exist yet.
	 * @param jsonParser JSON parser used for reading the initial config state
	 * @param exc Implicit execution context. Used in asynchronous config-saving.
	 * @param log Implicit logging implementation
	 * @return A new app config instance. Failure if JSON-parsing failed.
	 */
	def apply(path: Path, allowCreation: Boolean)
	         (implicit jsonParser: JsonParser, exc: ExecutionContext, log: Logger): Try[AppConfig] =
	{
		// Case: A new config file is appropriate => Creates an empty state
		if (allowCreation && path.notExists)
			Success(empty(path))
		else
			apply(path)
	}
	/**
	 * @param path Path which stores the current app configuration
	 * @param jsonParser JSON parser used for reading the initial config state
	 * @param exc Implicit execution context. Used in asynchronous config-saving.
	 * @param log Implicit logging implementation
	 * @return A new app config instance. Failure if JSON-parsing failed.
	 */
	def apply(path: Path)(implicit jsonParser: JsonParser, exc: ExecutionContext, log: Logger): Try[AppConfig] =
		path
			.tryReadWith { stream =>
				stream.notEmpty match {
					// Case: Non-empty file => Attempts to parse the file contents as JSON
					case Some(stream) => jsonParser(stream).flatMap { _.tryModel }
					// Case: Empty file => Continues with an empty initial model
					case None => Success(Model.empty)
				}
			}
			.map { new AppConfig(path, _) }
}

/**
 * Provides interactive access to a protected app-specific configuration JSON file.
 *
 * Note: Provides read & write access to nested properties, if a path is passed as a property name.
 *       E.g. a/b would point to property "b" of the model stored as property "a".
 *       Path parts may be separated with '/', ':' and '.'.
 *
 * @author Mikko Hilpinen
 * @since 09.01.2026, v2.8
 */
class AppConfig private(path: Path, initialState: Model)(implicit exc: ExecutionContext, log: Logger)
	extends PropertiesWrapper[Constant]
{
	// ATTRIBUTES   -------------------------
	
	private val pathSplitR = Regex.anyOf("/:.")
	private val fileAccess = new AccessQueue(path)
	
	private val modelP = Pointer.eventful(initialState)
	
	
	// INITIAL CODE -------------------------
	
	modelP.addLowPriorityListener { e =>
		fileAccess.async { _.writeJson(e.newValue).logWithMessage("Failed to record a new config state") }
		Continue
	}
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return This set of configurations as a single model
	 */
	def toModel = modelP.value
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def wrapped = toModel
	
	override def existingProperty(propName: String): Option[Constant] =
		access(propName)(super.existingProperty) { (model, keyView) =>
			model.flatMap { _.existingProperty(keyView.value) }
		}
	override def property(propName: String): Constant =
		access(propName)(super.property) { (model, keyView) =>
			model match {
				case Some(model) => model.property(keyView.value)
				case None => Constant(keyView.value, Value.empty)
			}
		}
	
	override def contains(propName: String): Boolean = access(propName)(super.contains) { (model, keyView) =>
		model.exists { _.contains(keyView.value) }
	}
	override def containsNonEmpty(propName: String): Boolean =
		access(propName)(super.containsNonEmpty) { (model, keyView) =>
			model.exists { _.containsNonEmpty(keyView.value) }
		}
	
	override def apply(propName: String): Value = access(propName)(super.apply) { (model, keyView) =>
		model match {
			case Some(model) => model(keyView.value)
			case None => Value.empty
		}
	}
	override def apply(propNames: IterableOnce[String]): Value = propNames.findMap(existingProperty) match {
		case Some(prop) => prop.value
		case None => Value.empty
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Assigns a value to a key
	 * @param key Targeted key / path
	 * @param value Assigned value
	 */
	def update(key: String, value: Value): Unit = update(key) { _ => value }
	/**
	 * Updates the value of a single key
	 * @param key Targeted key / path
	 * @param f A function used for modifying that key's value
	 */
	def update(key: String)(f: Mutate[Value]) = modelP.update { model =>
		key.split(pathSplitR).oneOrMany match {
			case Left(key) => model.mapValue(key)(f)
			case Right(path) => _map(model, path.view.dropRight(1).iterator, path.last)(f)
		}
	}
	
	/**
	 * @param key Targeted key
	 * @param value Value assigned, if this config doesn't specify a value for that key
	 * @return Value of the specified key
	 */
	def getOrElseUpdate(key: String)(value: => Value) = existingProperty(key) match {
		case Some(prop) => prop.value
		case None =>
			update(key, value)
			apply(key)
	}
	
	/**
	 * Removes a value from this config
	 * @param key Key of the value to remove
	 */
	def clear(key: String) = update(key, Value.empty)
	
	/**
	 * Modifies the value of a single nested property
	 * @param model Root model
	 * @param keysIter Iterator of the targeted keys (excluding the last key)
	 * @param lastKey The last targeted key
	 * @param f A function for altering the targeted value
	 * @return
	 */
	private def _map(model: Model, keysIter: Iterator[String], lastKey: String)(f: Mutate[Value]): Model = {
		keysIter.nextOption() match {
			// Case: More path to explore => Maps the next nested model
			case Some(nextKey) =>
				model.mapValue(nextKey) { value =>
					value.model match {
						case Some(nextModel) => _map(nextModel, keysIter, lastKey)(f)
						// Case: There's no nested model => Creates one
						case None => _create(keysIter ++ Single(lastKey), f(Value.empty))
					}
				}
			// Case: No more path to iterate => Maps the target value
			case None => model.mapValue(lastKey)(f)
		}
	}
	private def _create(keysIter: Iterator[String], value: Value): Value = keysIter.nextOption() match {
		case Some(nextKey) => Model.from(nextKey -> _create(keysIter, value))
		case None => value
	}
	
	private def access[A](path: String)(regular: String => A)(deep: (Option[Model], View[String]) => A) =
		path.split(pathSplitR).oneOrMany match {
			case Left(key) => regular(key)
			case Right(path) =>
				deep(path.view.dropRight(1)
					.foldLeftIterator[Option[Model]](Some(toModel)) { (model, key) => model.flatMap { _(key).model } }
					.takeTo { _.isEmpty }.last,
					View { path.last })
		}
}
