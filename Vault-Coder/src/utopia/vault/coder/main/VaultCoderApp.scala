package utopia.vault.coder.main

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.file.container.ObjectMapFileContainer
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.JSONReader
import utopia.flow.parse.json.{JSONReader, JsonParser}
import utopia.flow.time.Today
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.console.{ArgumentSchema, CommandArguments}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.vault.coder.controller.reader
import utopia.vault.coder.controller.writer.database.{AccessWriter, ColumnLengthRulesWriter, CombinedFactoryWriter, DbDescriptionAccessWriter, DbModelWriter, DescriptionLinkInterfaceWriter, FactoryWriter, InsertsWriter, SqlWriter, TablesWriter}
import utopia.vault.coder.controller.writer.documentation.DocumentationWriter
import utopia.vault.coder.controller.writer.model.{CombinedModelWriter, DescribedModelWriter, EnumerationWriter, ModelWriter}
import utopia.vault.coder.model.data.{Class, ClassReferences, Filter, NamingRules, ProjectData, ProjectPaths, ProjectSetup}
import utopia.vault.coder.model.enumeration.NameContext.FileName
import utopia.vault.coder.model.scala.datatype.Reference

import java.nio.file.{Path, Paths}
import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.io.{Codec, StdIn}
import scala.util.{Failure, Success, Try}

/**
  * The command line application for this project, which simply reads data from a json file and outputs it to a certain
  * location
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
object VaultCoderApp extends App
{
	DataType.setup()
	
	implicit val logger: Logger = SysErrLogger
	implicit val codec: Codec = Codec.UTF8
	implicit val jsonParser: JsonParser = JSONReader
	implicit val exc: ExecutionContext = new ThreadPool("vault-coder").executionContext
	lazy val projects = new ObjectMapFileContainer("projects.json", ProjectPaths)
	val arguments = CommandArguments(Vector(
		ArgumentSchema("project", "root", help = "Common directory path OR the name of an existing project"),
		ArgumentSchema("input", "in",
			help = "Path to file or directory where data is read from (relative to root, if root is specified)"),
		ArgumentSchema("output", "out",
			help = "Path to the directory where output data will be stored (relative to root, if root is specified)"),
		ArgumentSchema("target", "filter",
			help = "Search filter applied to written classes and/or enums (case-insensitive)"),
		ArgumentSchema("type", "group", help = "Specifies the group of items to write (all, class, enums or package)"),
		ArgumentSchema("merge", help = "Source origin where merge input files are read from (relative to root, if root is specified)"),
		ArgumentSchema.flag("all", "A", help = "Flag for selecting group 'all'"),
		ArgumentSchema.flag("single", "S", help = "Flag for limiting filter results to exact matches"),
		ArgumentSchema.flag("merging", "M", help = "Flag for enabling merge mode")),
		args.toVector)
	val startTime = LocalTime.now()
	
	// Writes hints and warnings
	if (arguments.unrecognized.nonEmpty)
		println(s"Warning! Following arguments were not recognized: ${arguments.unrecognized.mkString(", ")}")
	if (arguments.values.isEmpty)
	{
		println("Hint: This program supports following command line arguments:")
		arguments.schema.arguments.foreach { arg => println("- " + arg) }
		println()
	}
	
	// Checks if the specified root path is an alias
	val rootInput = arguments("root").string
	lazy val project = rootInput.flatMap(projects.get)
	val root = project match {
		case Some(p) => Right(p)
		case None => Left(rootInput.map { s => Roots(s).getOrElse { s: Path } })
	}
	val rootPath = root.leftOption.flatten
	rootPath.filter { _.notExists }.foreach { p =>
		println(s"Specified root path (${p.toAbsolutePath}) doesn't exist. Please try again.")
		System.exit(0)
	}
	
	def path(endPath: String): Path = rootPath match
	{
		case Some(root) => root/endPath
		case None => endPath
	}
	
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
								foundJsonFiles.foreach { p => println("\t- " + root.relativize(p)) }
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
	lazy val inputPath: Path = {
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
	lazy val outputPath: Path = project.map { _.outputDirectory }
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
	
	// Options for target type selection
	val _class = 1
	val _package = 2
	val _enums = 3
	val _all = 4
	val specifiedTargetType = {
		if (arguments("all").getBoolean)
			Some(_all)
		else
			arguments("type").string.map { _.toLowerCase }.flatMap {
				case "class" | "classes" => Some(_class)
				case "package" | "packages" => Some(_package)
				case "enums" | "enumerations" | "enum" | "enumeration" => Some(_enums)
				case "all" => Some(_all)
				case other =>
					println(s"Warning: Unrecognized target type '$other'")
					None
			}
	}
	lazy val filter = (arguments("filter").string match {
		case Some(filter) => filter.notEmpty
		case None =>
			if (specifiedTargetType.exists { _ != _all })
				StdIn.readNonEmptyLine(s"Please specify the ${
					arguments("type").getString} filter to use (leave empty if you want to target all of them)")
			else
				None
			// The filter may be more or less inclusive, based on the "single" flag
	}).map { filterText => Filter(filterText, arguments("single").getBoolean) }
	lazy val targetType = specifiedTargetType.getOrElse {
		filter match {
			case Some(filter) =>
				println(s"What kind of items do you want to target with filter '$filter'?")
				println("Available options: class | package | all")
				println("Default (empty) = all")
				StdIn.readLine().toLowerCase match
				{
					case "class" => _class
					case "package" => _package
					case "all" => _all
					case other =>
						println(s"Warning: '$other' is not a recognized option => treats as 'all'")
						_all
				}
			case None => _all
		}
	}
	
	lazy val mainMergeRoot = project.map { _.src }.orElse {
		arguments("merge").string match {
			case Some(mergeRoot) =>
				val mergeRootPath = path(mergeRoot)
				if (mergeRootPath.exists)
					Some(mergeRootPath)
				else {
					println(s"Specified merge source root path ${mergeRootPath.toAbsolutePath} doesn't exist")
					None
				}
			case None =>
				if (arguments("merging").getBoolean) {
					println("Please specify path to the existing source root directory (src)")
					println(s"Hint: Path may be absolute or relative to ${rootPath.getOrElse(Paths.get("")).toAbsolutePath}")
					StdIn.readNonEmptyLine().flatMap { input =>
						val mergeRoot = path(input)
						if (mergeRoot.exists)
							Some(mergeRoot)
						else {
							println(s"Specified path ${mergeRoot.toAbsolutePath} doesn't exist")
							None
						}
					}
				}
				else
					None
		}
	}
	lazy val alternativeMergeRoot: Option[Path] = project.map { _.altSrc }.getOrElse {
		if (mainMergeRoot.isDefined) {
			println("If you want, please specify the alternative merge source path for the other part of the project")
			println(s"The path may be absolute or relative to ${rootPath.getOrElse(Paths.get("")).toAbsolutePath}")
			StdIn.readNonEmptyLine().flatMap { input =>
				val mergeRoot = path(input)
				if (mergeRoot.exists)
					Some(mergeRoot)
				else
				{
					println(s"Specified directory ${mergeRoot.toAbsolutePath} didn't exist")
					None
				}
			}
		}
		else
			None
	}
	
	println()
	println(s"Reading class data from ${inputPath.toAbsolutePath}...")
	
	val didSucceed: Boolean = {
		if (inputPath.notExists) {
			println(s"Looks like no data can be found from $inputPath. Please try again with different input.")
			false
		}
		else if (inputPath.isDirectory)
			inputPath.children.flatMap { filePaths =>
				val jsonFilePaths = filePaths.filter { _.fileType.toLowerCase == "json" }
				println(s"Found ${jsonFilePaths.size} json file(s) from the input directory (${inputPath.fileName})")
				jsonFilePaths.tryMap { reader.ClassReader(_) }
			} match {
				// Groups read results that target the same project
				case Success(data) =>
					val groupedData = data.groupBy { p => (p.projectName, p.modelPackage, p.databasePackage) }
						.map { case ((pName, modelPackage, dbPackage), data) =>
							data.reduce { (a, b) =>
								val version = a.version match {
									case Some(aV) =>
										b.version match {
											case Some(bV) => Some(aV max bV)
											case None => Some(aV)
										}
									case None => b.version
								}
								ProjectData(pName, modelPackage, dbPackage, a.databaseName.orElse { b.databaseName },
									a.enumerations ++ b.enumerations, a.classes ++ b.classes,
									a.combinations ++ b.combinations, a.instances ++ b.instances, a.namingRules, version,
									a.modelCanReferToDB && b.modelCanReferToDB, a.prefixColumnNames && b.prefixColumnNames)
							}
						}
					filterAndWrite(groupedData)
				case Failure(error) =>
					error.printStackTrace()
					println("Class reading failed. Please make sure all of the files are in correct format.")
					false
			}
		else
		{
			if (inputPath.fileType.toLowerCase != "json")
				println(s"Warning: Expect file type is .json. Specified file is of type .${inputPath.fileType}")
			
			reader.ClassReader(inputPath) match {
				case Success(data) => filterAndWrite(Some(data))
				case Failure(error) =>
					error.printStackTrace()
					println("Class reading failed. Please make sure the file is in correct format.")
					false
			}
		}
	}
	
	// May store the project settings for future use
	if (didSucceed && project.isEmpty &&
		StdIn.ask("Do you want to save these settings to speed up program use next time?"))
	{
		println("What name do you want to give to this project?")
		rootInput.foreach { i => println(s"Default: $i") }
		StdIn.readNonEmptyLine().orElse(rootInput) match {
			case Some(projectName) =>
				mainMergeRoot
					.orElse { StdIn.readNonEmptyLine(
						s"Please specify the project source directory (absolute or relative to ${"".toAbsolutePath})")
						.map { p => p: Path } } match
				{
					case Some(src) =>
						val altSrc = alternativeMergeRoot.orElse {
							// Only requests the alternative merge root if the main merge root was not specified
							// during program startup (i.e. no alternative merge root was asked above)
							if (mainMergeRoot.isEmpty &&
								StdIn.ask("Do you want to specify an alternative source directory?"))
								StdIn.readNonEmptyLine(
									s"Please specify the alternative project source director (absolute or relative to ${"".toAbsolutePath})")
									.map { p => p: Path }
							else
								None
						}
						projects(projectName) = ProjectPaths(modelsPath, outputPath, src, altSrc)
						projects.activeSaveCompletionFuture.waitFor(3.seconds) match {
							case Success(_) => println(s"Saved the project. You may now refer to it as '$projectName'")
							case Failure(_) => println("Couldn't save the project.")
						}
					case None => println("Project saving cancelled")
				}
			case None => println("Project saving cancelled")
		}
	}
	
	def filterAndWrite(data: Iterable[ProjectData]): Boolean = {
		println()
		// Applies filters
		val filteredData = {
			if (targetType == _class)
				filter match {
					case Some(filter) => data.map { _.filterByClassName(filter) }
					case None => data.map { _.onlyClasses }
				}
			else if (targetType == _package)
				filter match {
					case Some(filter) => data.map { _.filterByPackage(filter) }
					case None => data
				}
			else if (targetType == _enums)
				filter match {
					case Some(filter) => data.map { _.filterByEnumName(filter) }
					case None => data.map { _.onlyEnumerations }
				}
			else
				filter match {
					case Some(filter) => data.map { _.filter(filter) }
					case None => data
				}
		}
		
		println()
		println(s"Read ${filteredData.map { _.classes.size }.sum } classes, ${
			filteredData.map { _.enumerations.size }.sum } enumerations and ${
			filteredData.map { _.combinations.size }.sum} combinations within ${filteredData.size} projects and ${
			filteredData.map { _.classes.map { _.packageName }.toSet.size }.sum } packages")
		println()
		println(s"Writing class and enumeration data to ${outputPath.toAbsolutePath}...")
		
		outputPath.asExistingDirectory.flatMap { directory =>
			// Moves the previously written files to a backup directory (which is cleared first)
			val backupDirectory = directory/"last-build"
			if (backupDirectory.exists)
				backupDirectory.deleteContents().failure.foreach { e => println(
					s"WARNING: failed to clear $backupDirectory before backup. Error message: ${e.getMessage}") }
			else
				backupDirectory.createDirectories().failure.foreach { _.printStackTrace() }
			directory.tryIterateChildren {
				_.filterNot { _ == backupDirectory }.map { _.moveTo(backupDirectory) }.toVector.toTry }
				.failure
				.foreach { e => println(
					s"WARNING: Failed to back up some previously built files. Error message: ${e.getMessage}") }
			
			// Handles one project at a time
			filteredData.tryForeach { data =>
				// Makes sure there is something to write
				if (data.isEmpty)
					Success(())
				else
				{
					println(s"Writing ${data.classes.size} classes, ${
						data.enumerations.size} enumerations and ${
						data.combinations.size } combinations for project ${data.projectName}${data.version match {
						case Some(version) => s" $version"
						case None => ""
					}}")
					implicit val naming: NamingRules = data.namingRules
					val mergeFileName = (data.projectName.inContext(FileName) ++ Vector("merge", "conflicts",
						Today.toString, startTime.getHour.toString, startTime.getMinute.toString)).fileName + ".txt"
					implicit val setup: ProjectSetup = ProjectSetup(data.projectName, data.modelPackage,
						data.databasePackage, directory,
						if (data.modelCanReferToDB) mainMergeRoot.toVector else
							Vector(mainMergeRoot, alternativeMergeRoot).flatten,
						directory/mergeFileName, data.version, data.modelCanReferToDB, data.prefixColumnNames)
					
					write(data)
				}
			}
		} match
		{
			case Success(_) =>
				println("All documents successfully written!")
				filteredData.exists { _.nonEmpty }
			case Failure(error) =>
				error.printStackTrace()
				println("Failed to write the documents. Please see error details above.")
				false
		}
	}
	
	def write(data: ProjectData)(implicit setup: ProjectSetup, naming: NamingRules): Try[Unit] =
	{
		def path(fileType: String, parts: String*) = {
			val fileNameBase = (setup.dbModuleName.inContext(FileName) ++ parts).fileName
			val fullFileName = data.version match {
				case Some(version) => s"$fileNameBase${naming(FileName).separator}$version.$fileType"
				case None => s"$fileNameBase.$fileType"
			}
			setup.sourceRoot/fullFileName
		}
		
		// Writes the enumerations
		data.enumerations.tryMap { EnumerationWriter(_) }
			// Writes the SQL declaration
			.flatMap { _ => SqlWriter(data.databaseName, data.classes,
				path("sql", "db", "structure")) }
			// Writes initial database inserts document
			.flatMap { _ => InsertsWriter(data.databaseName, data.instances,
				path("sql", "initial", "inserts")) }
			// Writes column length rules
			.flatMap { _ => ColumnLengthRulesWriter(data.databaseName, data.classes,
				path("json", "length", "rules")) }
			// Writes project documentation
			.flatMap { _ => DocumentationWriter(data, path("md")) }
			// Writes the tables document, which is referred to later, also
			.flatMap { _ => TablesWriter(data.classes) }
			.flatMap { tablesRef =>
				DescriptionLinkInterfaceWriter(data.classes, tablesRef).flatMap { descriptionLinkObjects =>
					// Next writes all required documents for each class
					data.classes.tryMap { write(_, tablesRef, descriptionLinkObjects) }.flatMap { classRefs =>
						// Finally writes the combined models
						val classRefsMap = classRefs.toMap
						data.combinations.tryForeach { combination =>
							val parentRefs = classRefsMap(combination.parentClass)
							val childRefs = classRefsMap(combination.childClass)
							CombinedModelWriter(combination, parentRefs.model, parentRefs.data, childRefs.model)
								.flatMap { combinedRefs =>
									CombinedFactoryWriter(combination, combinedRefs, parentRefs.factory,
										childRefs.factory)
										.map { _ => () }
								}
						}
					}
				}
			}
	}
	
	def write(classToWrite: Class, tablesRef: Reference,
	          descriptionLinkObjects: Option[(Reference, Reference, Reference)])
	         (implicit setup: ProjectSetup, naming: NamingRules): Try[(Class, ClassReferences)] =
	{
		ModelWriter(classToWrite).flatMap { case (modelRef, dataRef) =>
			FactoryWriter(classToWrite, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
				DbModelWriter(classToWrite, modelRef, dataRef, factoryRef)
					.flatMap { dbModelRef =>
						// Adds description-specific references if applicable
						(descriptionLinkObjects match {
							// Case: At least one class uses descriptions
							case Some((linkModels, _, linkedDescriptionFactories)) =>
								classToWrite.descriptionLinkClass match {
									case Some(descriptionLinkClass) =>
										DescribedModelWriter(classToWrite, modelRef).flatMap { describedRef =>
											DbDescriptionAccessWriter(descriptionLinkClass,
												classToWrite.name, linkModels, linkedDescriptionFactories)
												.map { case (singleAccessRef, manyAccessRef) =>
													Some(describedRef, singleAccessRef, manyAccessRef)
												}
										}
									case None => Success(None)
								}
							// Case: No classes use descriptions => automatically succeeds
							case None => Success(None)
						}).flatMap { descriptionReferences =>
							// Finally writes the access points
							AccessWriter(classToWrite, modelRef, factoryRef, dbModelRef,
								descriptionReferences)
								.map { _ => classToWrite -> ClassReferences(modelRef, dataRef, factoryRef, dbModelRef) }
						}
					}
			}
		}
	}
}
