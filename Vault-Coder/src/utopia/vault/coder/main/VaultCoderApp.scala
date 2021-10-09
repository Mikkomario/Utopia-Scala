package utopia.vault.coder.main

import utopia.flow.generic.DataType
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.vault.coder.controller.ClassReader
import utopia.vault.coder.controller.writer.{AccessWriter, DbDescriptionInteractionsWriter, DbModelWriter, DescribedModelWriter, EnumerationWriter, FactoryWriter, ModelWriter, SqlWriter, TablesWriter}
import utopia.vault.coder.model.data.{Class, Enum, ProjectSetup}

import java.nio.file.Path
import scala.io.Codec
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
	
	val rootPath = if (args.isEmpty) None else Some(args.head: Path)
	def path(endPath: String): Path = rootPath match
	{
		case Some(root) => root/endPath
		case None => endPath
	}
	
	val inputPath: Path = if (args.length < 2) rootPath.getOrElse("input") else path(args(1))
	val outputPath: Path = path(if (args.length < 3) "output" else args(2))
	
	def write(data: Map[String, (Vector[Class], Vector[Enum])]) =
	{
		println(s"Read ${data.valuesIterator.map { _._1.size }.sum } classes and ${
			data.valuesIterator.map { _._2.size }.sum } enumerations within ${data.size} projects and ${
			data.valuesIterator.map { _._1.map { _.packageName }.distinct.size }.sum } packages")
		println(s"Writing class and enumeration data to ${outputPath.toAbsolutePath}...")
		if (args.length < 2)
			println("Hint: You can customize the output path by specifying it as the second command line argument")
		
		outputPath.asExistingDirectory.flatMap { rootDirectory =>
			// Handles one project at a time
			data.tryForeach { case (basePackageName, (classes, enumerations)) =>
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
							// Next writes all required documents for each class
							classes.tryForeach { classToWrite =>
								ModelWriter(classToWrite).flatMap { case (modelRef, dataRef) =>
									FactoryWriter(classToWrite, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
										DbModelWriter(classToWrite, modelRef, dataRef, factoryRef)
											.flatMap { dbModelRef =>
												AccessWriter(classToWrite, modelRef, factoryRef, dbModelRef)
											}
									}.flatMap { _ =>
										// May also write description-related documents
										classToWrite.descriptionLinkClass.tryForeach { descriptionLinkClass =>
											DescribedModelWriter(classToWrite, modelRef).flatMap { _ =>
												DbDescriptionInteractionsWriter(descriptionLinkClass, tablesRef,
													classToWrite.name)
													.map { _ => () }
											}
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
	
	println(s"Reading class data from ${inputPath.toAbsolutePath}...")
	if (args.isEmpty)
	{
		println("Hint: You can customize the read location by specifying it as the second command line argument")
		println("The first command line argument is the root path for both the input (2) and output (3) path arguments")
	}
	
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
