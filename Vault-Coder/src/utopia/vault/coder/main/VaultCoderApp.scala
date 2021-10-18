package utopia.vault.coder.main

import utopia.flow.generic.DataType
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.console.{ArgumentSchema, CommandArguments}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.ClassReader
import utopia.vault.coder.controller.writer.database.{AccessWriter, CombinedFactoryWriter, DbDescriptionAccessWriter, DbModelWriter, DescriptionLinkInterfaceWriter, FactoryWriter, SqlWriter, TablesWriter}
import utopia.vault.coder.controller.writer.model.{CombinedModelWriter, DescribedModelWriter, EnumerationWriter, ModelWriter}
import utopia.vault.coder.model.data.{Class, ClassReferences, Filter, ProjectData, ProjectSetup}
import utopia.vault.coder.model.scala.Reference

import java.nio.file.{Path, Paths}
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
	
	implicit val codec: Codec = Codec.UTF8
	implicit val jsonParser: JsonParser = JSONReader
	val arguments = CommandArguments(Vector(
		ArgumentSchema("root", "path", help = "Common directory path for both input and output"),
		ArgumentSchema("input", "in",
			help = "Path to file or directory where data is read from (relative to root, if root is specified)"),
		ArgumentSchema("output", "out",
			help = "Path to the directory where output data will be stored (relative to root, if root is specified)"),
		ArgumentSchema("target", "filter",
			help = "Search filter applied to written classes and/or enums (case-insensitive)"),
		ArgumentSchema("type", "group", help = "Specifies the group of items to write (all, class, enums or package)"),
		ArgumentSchema.flag("all", "A", help = "Flag for selecting group 'all'"),
		ArgumentSchema.flag("single", "S", help = "Flag for limiting filter results to exact matches")),
		args.toVector)
	
	// Writes hints and warnings
	if (arguments.unrecognized.nonEmpty)
		println(s"Warning! Following arguments were not recognized: ${arguments.unrecognized.mkString(", ")}")
	if (arguments.values.isEmpty)
	{
		println("Hint: This program supports following command line arguments:")
		arguments.schema.arguments.foreach { arg => println("- " + arg) }
		println()
	}
	
	val rootPath = arguments("root").string.map[Path] { s => s }
	rootPath.filter { _.notExists }.foreach { p =>
		println(s"Specified root path (${p.toAbsolutePath}) doesn't exist. Please try again.")
		System.exit(0)
	}
	
	def path(endPath: String): Path = rootPath match
	{
		case Some(root) => root/endPath
		case None => endPath
	}
	
	lazy val inputPath: Path = arguments("input").string.map(path).getOrElse {
		rootPath match
		{
			case Some(root) =>
				println(s"Please type a directory or a .json file path relative to ${root.toAbsolutePath}")
				val foundJsonFiles = root.allChildrenIterator.flatMap { _.toOption }
					.filter { p => p.isRegularFile && p.fileType == "json" }.take(5).toVector
				if (foundJsonFiles.nonEmpty)
				{
					println("Some .json files that were found:")
					foundJsonFiles.foreach { p => println("- " + root.relativize(p)) }
				}
				root/StdIn.readLine()
			case None =>
				println("Please type a directory or a .json file path")
				println(s"The path may be absolute or relative to ${Paths.get("").toAbsolutePath}")
				StdIn.readLine()
		}
	}
	lazy val outputPath: Path = arguments("output").string.map(path).getOrElse {
		rootPath match
		{
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
	val specifiedTargetType =
	{
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
	lazy val filter = (arguments("filter").string match
	{
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
		filter match
		{
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
	
	println()
	println(s"Reading class data from ${inputPath.toAbsolutePath}...")
	
	if (inputPath.notExists)
		println("Looks like no data can be found from that location. Please try again with different input.")
	else if (inputPath.isDirectory)
		inputPath.children.flatMap { filePaths =>
			val jsonFilePaths = filePaths.filter { _.fileType.toLowerCase == "json" }
			println(s"Found ${jsonFilePaths.size} json file(s) from the input directory (${inputPath.fileName})")
			jsonFilePaths.tryMap { ClassReader(_) }
		} match {
			// Groups read results that target the same base package
			case Success(data) =>
				val groupedData = data.groupBy { _.basePackage }.map { case (basePackage, data) =>
					data.reduce { (a, b) => ProjectData(basePackage, a.enumerations ++ b.enumerations,
						a.classes ++ b.classes, a.combinations ++ b.combinations) }
				}
				filterAndWrite(groupedData)
			case Failure(error) =>
				error.printStackTrace()
				println("Class reading failed. Please make sure all of the files are in correct format.")
		}
	else
	{
		if (inputPath.fileType.toLowerCase != "json")
			println(s"Warning: Expect file type is .json. Specified file is of type .${inputPath.fileType}")
		
		ClassReader(inputPath) match
		{
			case Success(data) => filterAndWrite(Some(data))
			case Failure(error) =>
				error.printStackTrace()
				println("Class reading failed. Please make sure the file is in correct format.")
		}
	}
	
	def filterAndWrite(data: Iterable[ProjectData]): Unit =
	{
		println()
		// Applies filters
		val filteredData =
		{
			if (targetType == _class)
				filter match
				{
					case Some(filter) => data.map { _.filterByClassName(filter) }
					case None => data.map { _.onlyClasses }
				}
			else if (targetType == _package)
				filter match
				{
					case Some(filter) => data.map { _.filterByPackage(filter) }
					case None => data
				}
			else if (targetType == _enums)
				filter match
				{
					case Some(filter) => data.map { _.filterByEnumName(filter) }
					case None => data.map { _.onlyEnumerations }
				}
			else
				filter match
				{
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
		
		outputPath.asExistingDirectory.flatMap { rootDirectory =>
			// Handles one project at a time
			filteredData.tryForeach { data =>
				// Makes sure there is something to write
				if (data.isEmpty)
					Success(())
				else
				{
					println(s"Writing ${data.classes.size} classes, ${
						data.enumerations.size} enumerations and ${
						data.combinations.size } combinations for project ${data.basePackage}")
					val directory = if (data.basePackage.isEmpty) Success(rootDirectory) else
						(rootDirectory/data.basePackage.parts.mkString("-")).asExistingDirectory
					directory.flatMap { directory =>
						implicit val setup: ProjectSetup = ProjectSetup(data.basePackage, directory)
						write(data)
					}
				}
			}
		} match
		{
			case Success(_) => println("All documents successfully written!")
			case Failure(error) =>
				error.printStackTrace()
				println("Failed to write the documents. Please see error details above.")
		}
	}
	
	def write(data: ProjectData)(implicit setup: ProjectSetup): Try[Unit] =
	{
		// Writes the enumerations
		data.enumerations.tryMap { EnumerationWriter(_) }
			// Next writes the SQL declaration and the tables document
			.flatMap { _ => SqlWriter(data.classes, setup.sourceRoot/"db_structure.sql") }
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
	
	def write(classToWrite: Class, tablesRef: Reference, descriptionLinkObjects: Option[(Reference, Reference)])
	         (implicit setup: ProjectSetup): Try[(Class, ClassReferences)] =
	{
		ModelWriter(classToWrite).flatMap { case (modelRef, dataRef) =>
			FactoryWriter(classToWrite, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
				DbModelWriter(classToWrite, modelRef, dataRef, factoryRef)
					.flatMap { dbModelRef =>
						// Adds description-specific references if applicable
						(descriptionLinkObjects match
						{
							// Case: At least one class uses descriptions
							case Some((linkModels, linkFactories)) =>
								classToWrite.descriptionLinkClass match
								{
									case Some(descriptionLinkClass) =>
										DescribedModelWriter(classToWrite, modelRef).flatMap { describedRef =>
											DbDescriptionAccessWriter(descriptionLinkClass,
												classToWrite.name, linkModels, linkFactories)
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
