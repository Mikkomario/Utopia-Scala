package utopia.coder.controller.app

import utopia.coder.model.data.{Filter, ProjectPaths}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.container.ObjectMapFileContainer
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.console.{ArgumentSchema, CommandArguments}
import utopia.flow.util.logging.Logger
import utopia.flow.util.{NotEmpty, Version}
import utopia.flow.view.immutable.caching.Lazy

import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * Abstract coder application logic which reads one or more input files and generates code files
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
trait CoderAppLogic extends AppLogic
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return The execution context used during application runtime
	  */
	protected implicit def exc: ExecutionContext
	/**
	  * @return Logging implementation used for encountered errors
	  */
	protected implicit def log: Logger
	
	/**
	  * @return Path to the json file where stored projects are listed
	  */
	protected def projectsStoreLocation: Path
	/**
	  * @return Whether alternative merge roots should be asked from the user, if otherwise appropriate
	  */
	protected def supportsAlternativeMergeRoots: Boolean
	/**
	  * Runs this application
	  * @param args Command line arguments specified by the user
	  * @param inputPath Data input path (either a json file or a directory)
	  * @param outputPath Output write path
	  * @param mergeRoots Source code directories for merging
	  * @param filter A filter to apply
	  * @param targetGroup Targeted subgroup. None if all groups are targeted.
	  * @return Whether the parsing process succeeded
	  */
	protected def run(args: CommandArguments, inputPath: Lazy[Path], outputPath: Lazy[Path],
	                  mergeRoots: Lazy[Vector[Path]], filter: Lazy[Option[Filter]], targetGroup: Option[String]): Boolean
	
	
	// IMPLEMENTED  --------------------------
	
	override def argumentSchema = Vector(
		ArgumentSchema("project", "root", help = "Common directory path OR the name of an existing project"),
		ArgumentSchema("input", "in",
			help = "Path to file or directory where data is read from (relative to root, if root is specified)"),
		ArgumentSchema("output", "out",
			help = "Path to the directory where output data will be stored (relative to root, if root is specified)"),
		ArgumentSchema("target", "filter",
			help = "Search filter applied to written classes and/or enums (case-insensitive)"),
		ArgumentSchema("type", "group", help = "Specifies the group of items to write, or the type of filtering applied"),
		ArgumentSchema("merge", help = "Source origin where merge input files are read from (relative to root, if root is specified)"),
		ArgumentSchema.flag("all", "A", help = "Flag for selecting group 'all'"),
		ArgumentSchema.flag("single", "S", help = "Flag for limiting filter results to exact matches"),
		ArgumentSchema.flag("merging", "M", help = "Flag for enabling merge mode"),
		ArgumentSchema.flag("nomerge", "N", help = "Flag for disabling merge mode")
	)
	
	override def apply(arguments: CommandArguments) = {
		implicit val jsonParser: JsonParser = JsonReader
		lazy val projects = new ObjectMapFileContainer(projectsStoreLocation, ProjectPaths)
		
		// Checks if the specified root path is an alias
		val rootInput = arguments("root").string
		lazy val project = rootInput.flatMap(projects.get)
		val root = project match {
			case Some(p) => Right(p)
			case None => Left(rootInput.map { s => s: Path })
		}
		val rootPath = root.leftOption.flatten
		rootPath.filter { _.notExists }.foreach { p =>
			println(s"Specified root path (${p.toAbsolutePath}) doesn't exist. Please try again.")
			System.exit(0)
		}
		def path(endPath: String): Path = rootPath match {
			case Some(root) => root/endPath
			case None => endPath
		}
		
		// Determines the input path
		lazy val modelsPath: Path = project match {
			case Some(p) => p.modelsDirectory
			case None =>
				arguments("input").string.map(path).getOrElse {
					rootPath match {
						case Some(root) =>
							println(s"Please type a models directory or a .json file path relative to ${root.toAbsolutePath}")
							val foundJsonFiles = root.allChildrenIterator.flatMap { _.toOption }
								.filter { _.fileType == "json" }.take(10).toVector
							if (foundJsonFiles.nonEmpty) {
								if (foundJsonFiles.map { _.parent }.areAllEqual)
									println(s"Suggested directory: ${root.relativize(foundJsonFiles.head.parent)}")
								else {
									println("Some .json files that were found:")
									foundJsonFiles.foreach { p => println(s"\t- ${ root.relativize(p) }") }
								}
							}
							root/StdIn.readLine()
						case None =>
							println("Please type the models directory or a .json file path")
							println(s"The path may be absolute or relative to ${"".toAbsolutePath}")
							StdIn.readLine(): Path
					}
				}
		}
		val inputPath: Lazy[Path] = Lazy {
			if (modelsPath.fileType == "json")
				modelsPath
			else {
				modelsPath.children match {
					case Success(children) =>
						val jsonChildren = children.filter { _.fileType == "json" }
						if (jsonChildren.nonEmpty)
							jsonChildren.flatMap { p => Version.findFrom(p.fileName.untilLast(".")).map { _ -> p } }
								.maxByOption { _._1 }.map { _._2 }
								.getOrElse { modelsPath }
						else {
							val subDirectories = children.filter { _.isDirectory }
							subDirectories.flatMap { p => Version.findFrom(p.fileName).map { _ -> p } }
								.maxByOption { _._1 }.map { _._2 }
								.getOrElse {
									println("Please specify the models.json file to read (or a directory containing multiple of such files)")
									println(s"Instruction: Specify the path relative to $modelsPath")
									if (subDirectories.isEmpty)
										println(s"Warning: No suitable files were found from $modelsPath")
									else {
										println("Available subdirectories:")
										subDirectories.foreach { p => println(s"\t- ${p.fileName}") }
									}
									modelsPath/StdIn.readLine()
								}
						}
					case Failure(_) =>
						println("Please specify the models.json file to read (or a directory containing multiple of such files)")
						println(s"Instruction: Specify the path relative to $modelsPath")
						modelsPath/StdIn.readLine()
				}
			}
		}
		// Determines the output path
		lazy val outputPath: Lazy[Path] = Lazy {
			project.map { _.outputDirectory }
				.orElse { arguments("output").string.map(path) }
				.getOrElse {
					rootPath match {
						case Some(root) =>
							println(s"Please specify the output directory path relative to ${root.toAbsolutePath}")
							root/StdIn.readLine()
						case None =>
							println("Please specify the output directory path")
							println(s"The path may be absolute or relative to ${Paths.get("").toAbsolutePath}")
							StdIn.readLine()
					}
				}
		}
		// Determines the target group and the filter
		lazy val specificallyTargetedType = arguments("type").string.filterNot { _.isEmpty }
		val targetsAllTypes = arguments("all").getBoolean || specificallyTargetedType.contains("all")
		val filter = Lazy {
			(arguments("filter").string match {
				case Some(filter) => filter.notEmpty
				case None =>
					if (!targetsAllTypes && specificallyTargetedType.isDefined)
						StdIn.readNonEmptyLine(s"Please specify the ${
							arguments("type").getString} filter to use (leave empty if you want to target all of them)")
					else
						None
				// The filter may be more or less inclusive, based on the "single" flag
			}).map { filterText => Filter(filterText, arguments("single").getBoolean) }
		}
		val targetType = if (targetsAllTypes) None else specificallyTargetedType
		// Determines merge roots
		val mergeRoots = Lazy {
			val mainRoot = project.map { _.src }.orElse {
				arguments("merge").string match {
					case Some(mergeRoot) =>
						val mergeRootPath = path(mergeRoot)
						if (mergeRootPath.exists)
							Some(mergeRootPath)
						else {
							println(s"Specified merge source root path ${ mergeRootPath.toAbsolutePath } doesn't exist")
							None
						}
					case None =>
						if (arguments("merging").getBoolean) {
							println("Please specify path to the existing source root directory (src)")
							println(s"Hint: Path may be absolute or relative to ${
								rootPath.getOrElse(Paths.get("")).toAbsolutePath }")
							StdIn.readNonEmptyLine().flatMap { input =>
								val mergeRoot = path(input)
								if (mergeRoot.exists)
									Some(mergeRoot)
								else {
									println(s"Specified path ${ mergeRoot.toAbsolutePath } doesn't exist")
									None
								}
							}
						}
						else
							None
				}
			}
			val alternativeMergeRoot: Option[Path] = project.map { _.altSrc }.getOrElse {
				if (mainRoot.isDefined && supportsAlternativeMergeRoots) {
					println("If you want, please specify the alternative merge source path for the project")
					println(s"The path may be absolute or relative to ${
						rootPath.getOrElse(Paths.get("")).toAbsolutePath }")
					StdIn.readNonEmptyLine().flatMap { input =>
						val mergeRoot = path(input)
						if (mergeRoot.exists)
							Some(mergeRoot)
						else {
							println(s"Specified directory ${ mergeRoot.toAbsolutePath } didn't exist")
							None
						}
					}
				}
				else
					None
			}
			Vector(mainRoot, alternativeMergeRoot).flatten
		}
		
		// Runs the actual application logic
		// Merge roots may not be given if specifically denied with -N
		val didSucceed = run(arguments, inputPath, outputPath,
			if (arguments("nomerge").getBoolean) Lazy.initialized(Vector()) else mergeRoots, filter, targetType)
		
		// May store the project settings for future use
		if (didSucceed && project.isEmpty &&
			StdIn.ask("Do you want to save these settings to speed up program use next time?"))
		{
			println("What name do you want to give to this project?")
			val defaultName = rootInput.map { input =>
				if (input.contains('/'))
					input.afterLast("/")
				else if (input.contains('\\'))
					input.afterLast("\\")
				else
					input
			}
			defaultName.foreach { i => println(s"Default: $i") }
			StdIn.readNonEmptyLine().orElse(defaultName) match {
				case Some(projectName) =>
					// The project merge root(s) are required
					val projectMergeRoots = NotEmpty(mergeRoots.value).orElse {
						StdIn.readNonEmptyLine(
							s"Please specify the project source directory (absolute or relative to ${ "".toAbsolutePath })")
							.map { p => p: Path }
							.map { mainRoot =>
								val altSrc = {
									if (supportsAlternativeMergeRoots &&
										StdIn.ask("Do you want to specify an alternative source directory?"))
										StdIn.readNonEmptyLine(
											s"Please specify the alternative project source director (absolute or relative to ${ "".toAbsolutePath })")
											.map { p => p: Path }
									else
										None
								}
								Vector(mainRoot) ++ altSrc
							}
					}
					projectMergeRoots match {
						case Some(mergeRoots) =>
							projects(projectName) = ProjectPaths(modelsPath, outputPath.value, mergeRoots.head,
								mergeRoots.lift(1))
							projects.activeSaveCompletionFuture.waitFor(3.seconds) match {
								case Success(_) => println(s"Saved the project. You may now refer to it as '$projectName'")
								case Failure(_) => println("Couldn't save the project.")
							}
						case None => println("Project saving cancelled")
					}
				case None => println("Project saving cancelled")
			}
		}
	}
}
