package utopia.vault.coder.main

import utopia.flow.generic.DataType
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.console.{ArgumentSchema, CommandArguments}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.ClassReader
import utopia.vault.coder.controller.writer.{AccessWriter, DbDescriptionAccessWriter, DbModelWriter, DescribedModelWriter, DescriptionLinkInterfaceWriter, EnumerationWriter, FactoryWriter, ModelWriter, SqlWriter, TablesWriter}
import utopia.vault.coder.model.data.{Class, Enum, ProjectSetup}

import java.nio.file.{Path, Paths}
import scala.io.{Codec, StdIn}
import scala.util.{Failure, Success}

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
		ArgumentSchema.flag("all", "A", help = "Flag for selecting group 'all'")),
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
			if (specifiedTargetType.exists { t => t != _all && t != _enums })
				StdIn.readNonEmptyLine(s"Please specify the ${
					arguments("type").getString} filter to use (leave empty if you want to target all of them)")
			else
				None
	}).map { _.toLowerCase }
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
	
	def write(data: Map[String, (Vector[Class], Vector[Enum])]) =
	{
		println()
		// Applies filters
		val filteredData =
		{
			if (targetType == _class)
				filter match
				{
					case Some(filter) =>
						data.map { case (base, (classes, _)) => base ->
							(classes.filter { _.name.variants.exists { _.toLowerCase.contains(filter) } }, Vector())
						}
					case None => data.map { case (base, (classes, _)) => base -> (classes, Vector()) }
				}
			else if (targetType == _package)
				filter match
				{
					case Some(filter) =>
						data.map { case (base, (classes, _)) =>
							base -> (classes.filter { _.packageName.toLowerCase.contains(filter) }, Vector())
						}
					case None => data
				}
			else if (targetType == _enums)
				filter match
				{
					case Some(filter) =>
						data.map { case (base, (_, enums)) =>
							base -> (Vector(), enums.filter { _.name.toLowerCase.contains(filter) })
						}
					case None => data.map { case (base, (_, enums)) => base -> (Vector(), enums) }
				}
			else
				filter match
				{
					case Some(filter) =>
						data.map { case (base, (classes, enums)) =>
							val filteredEnums = enums.filter { _.name.toLowerCase.contains(filter) }
							val filteredClasses = classes.filter { c => c.packageName.toLowerCase.contains(filter) ||
								c.name.variants.exists { _.toLowerCase.contains(filter) } }
							base -> (filteredClasses, filteredEnums)
						}
					case None => data
				}
		}
		
		println()
		println(s"Read ${filteredData.valuesIterator.map { _._1.size }.sum } classes and ${
			filteredData.valuesIterator.map { _._2.size }.sum } enumerations within ${filteredData.size} projects and ${
			filteredData.valuesIterator.map { _._1.map { _.packageName }.distinct.size }.sum } packages")
		println()
		println(s"Writing class and enumeration data to ${outputPath.toAbsolutePath}...")
		
		outputPath.asExistingDirectory.flatMap { rootDirectory =>
			// Handles one project at a time
			filteredData.tryForeach { case (basePackageName, (classes, enumerations)) =>
				// Makes sure there is something to write
				if (classes.isEmpty && enumerations.isEmpty)
					Success(())
				else
				{
					println(s"Writing ${classes.size} classes and ${
						enumerations.size} enumerations for project $basePackageName")
					val directory = if (basePackageName.isEmpty) Success(rootDirectory) else
						(rootDirectory/basePackageName.replace('.', '-')).asExistingDirectory
					directory.flatMap { directory =>
						implicit val setup: ProjectSetup = ProjectSetup(basePackageName, directory)
						// Writes the enumerations
						enumerations.tryMap { EnumerationWriter(_) }
							// Next writes the SQL declaration and the tables document
							.flatMap { _ => SqlWriter(classes, directory/"db_structure.sql") }
							.flatMap { _ => TablesWriter(classes) }
							.flatMap { tablesRef =>
								DescriptionLinkInterfaceWriter(classes, tablesRef).flatMap { descriptionLinkObjects =>
									// Next writes all required documents for each class
									classes.tryMap { classToWrite =>
										ModelWriter(classToWrite).flatMap { case (modelRef, dataRef) =>
											FactoryWriter(classToWrite, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
												DbModelWriter(classToWrite, modelRef, dataRef, factoryRef)
													.flatMap { dbModelRef =>
														AccessWriter(classToWrite, modelRef, factoryRef, dbModelRef)
															.map { _ => classToWrite -> modelRef }
													}
											}
										}
									}.flatMap { classesWithModels =>
										// May also write description-related documents
										descriptionLinkObjects match
										{
											// Case: At least one class uses descriptions
											case Some((linkModels, linkFactories)) =>
												classesWithModels.tryForeach { case (classToWrite, modelRef) =>
													classToWrite.descriptionLinkClass.tryForeach { descriptionLinkClass =>
														DescribedModelWriter(classToWrite, modelRef).flatMap { _ =>
															DbDescriptionAccessWriter(descriptionLinkClass,
																classToWrite.name, linkModels, linkFactories)
																.map { _ => () }
														}
													}
												}
											// Case: No classes use descriptions => automatically succeeds
											case None => Success(())
										}
									}
								}
							}
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
				write(data.groupMapReduce { _._1 } { case (_, enums, classes) => classes -> enums } {
					case ((classes1, enums1), (classes2, enums2)) => (classes1 ++ classes2) -> (enums1 ++ enums2)
				})
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
			case Success((basePackage, enums, classes)) => write(Map(basePackage -> (classes -> enums)))
			case Failure(error) =>
				error.printStackTrace()
				println("Class reading failed. Please make sure the file is in correct format.")
		}
	}
}
