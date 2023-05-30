package utopia.vault.coder.main

import utopia.coder.controller.app.CoderAppLogic
import utopia.coder.model.data.{Filter, NamingRules}
import utopia.coder.model.enumeration.NameContext.FileName
import utopia.coder.model.scala.datatype.Reference
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.Today
import utopia.flow.util.console.CommandArguments
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.coder.controller.reader
import utopia.vault.coder.controller.writer.database._
import utopia.vault.coder.controller.writer.documentation.DocumentationWriter
import utopia.vault.coder.controller.writer.model.{CombinedModelWriter, DescribedModelWriter, EnumerationWriter, ModelWriter}
import utopia.vault.coder.model.data.{Class, ClassReferences, CombinationData, ProjectData, VaultProjectSetup}
import utopia.vault.coder.util.Common

import java.nio.file.Path
import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.util.{Failure, Success, Try}

/**
  * The main Vault-Coder application logic which reads one or more input files and generates class files,
  * tables, documentation, etc.
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
object MainAppLogic extends CoderAppLogic
{
	// ATTRIBUTES   ----------------------
	
	private implicit val codec: Codec = Codec.UTF8
	
	// Options for target type selection
	private val _class = 1
	private val _package = 2
	private val _enums = 3
	private val _all = 4
	
	override val name = "main"
	
	
	// IMPLEMENTED  ----------------------
	
	override protected implicit def exc: ExecutionContext = Common.exc
	override protected implicit def log: Logger = Common.log
	
	override protected def projectsStoreLocation: Path = "projects.json"
	override protected def supportsAlternativeMergeRoots: Boolean = true
	
	override protected def run(args: CommandArguments, inputPath: Lazy[Path], outputPath: Lazy[Path],
	                           mergeRoots: Lazy[Vector[Path]], filter: Lazy[Option[Filter]],
	                           targetGroup: Option[String]): Boolean =
	{
		lazy val targetType = targetGroup match {
			case Some(specifiedGroup) =>
				specifiedGroup.toLowerCase match {
					case "class" | "classes" => _class
					case "package" | "packages" => _package
					case "enums" | "enumerations" | "enum" | "enumeration" => _enums
					case "all" => _all
					case other =>
						println(s"Warning: Unrecognized target type '$other' => Targets all types")
						_all
				}
			case None => _all
		}
		
		if (inputPath.value.notExists) {
			println(s"Looks like no data can be found from $inputPath. Please try again with different input.")
			false
		}
		else if (inputPath.value.isDirectory)
			inputPath.value.children.flatMap { filePaths =>
				val jsonFilePaths = filePaths.filter { _.fileType.toLowerCase == "json" }
				println(s"Found ${ jsonFilePaths.size } json file(s) from the input directory (${ inputPath.value.fileName })")
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
					filterAndWrite(groupedData, targetType, filter.value, outputPath.value, mergeRoots.value.headOption,
						mergeRoots.value.lift(1), args)
				case Failure(error) =>
					error.printStackTrace()
					println("Class reading failed. Please make sure all of the files are in correct format.")
					false
			}
		else {
			if (inputPath.value.fileType.toLowerCase != "json")
				println(s"Warning: Expect file type is .json. Specified file is of type .${ inputPath.value.fileType }")
			
			reader.ClassReader(inputPath.value) match {
				case Success(data) => filterAndWrite(Some(data), targetType, filter.value, outputPath.value,
					mergeRoots.value.headOption, mergeRoots.value.lift(1), args)
				case Failure(error) =>
					error.printStackTrace()
					println("Class reading failed. Please make sure the file is in correct format.")
					false
			}
		}
	}
	
	
	// OTHER    -------------------
	
	
	private def filterAndWrite(data: Iterable[ProjectData], targetType: => Int, filter: => Option[Filter],
	                           outputPath: => Path, mainMergeRoot: => Option[Path],
	                           alternativeMergeRoot: => Option[Path], arguments: CommandArguments): Boolean =
	{
		val startTime = LocalTime.now()
		
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
					val mergeFileName = s"${
						(data.projectName.inContext(FileName) ++ Vector("merge", "conflicts",
							Today.toString, startTime.getHour.toString, startTime.getMinute.toString)).fileName
					}.txt"
					val mergePaths = {
						// Case: Merging is disabled
						if (arguments("nomerge").getBoolean)
							Vector()
						// Case: Single module project
						else if (data.modelCanReferToDB)
							mainMergeRoot.toVector
						// Case: Multi-module project
						else
							Vector(mainMergeRoot, alternativeMergeRoot).flatten
					}
					implicit val setup: VaultProjectSetup = VaultProjectSetup(data.projectName, data.modelPackage,
						data.databasePackage, directory, mergePaths,
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
	
	private def write(data: ProjectData)(implicit setup: VaultProjectSetup, naming: NamingRules): Try[Unit] =
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
						data.combinations.tryForeach { writeCombo(_, classRefsMap) }
					}
				}
			}
	}
	
	private def write(classToWrite: Class, tablesRef: Reference,
	          descriptionLinkObjects: Option[(Reference, Reference, Reference)])
	         (implicit setup: VaultProjectSetup, naming: NamingRules): Try[(Class, ClassReferences)] =
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
								.map { case (genericUniqueAccessRef, genericManyAccessRef) =>
									classToWrite -> ClassReferences(modelRef, dataRef, factoryRef, dbModelRef,
										genericUniqueAccessRef, genericManyAccessRef) }
						}
					}
			}
		}
	}
	
	private def writeCombo(combination: CombinationData, classRefsMap: Map[Class, ClassReferences])
	                      (implicit setup: VaultProjectSetup, naming: NamingRules) =
	{
		val parentRefs = classRefsMap(combination.parentClass)
		val childRefs = classRefsMap(combination.childClass)
		CombinedModelWriter(combination, parentRefs.model, parentRefs.data, childRefs.model).flatMap { combinedRefs =>
			CombinedFactoryWriter(combination, combinedRefs, parentRefs.factory, childRefs.factory)
				.flatMap { comboFactoryRef =>
					parentRefs.genericUniqueAccessTrait
						.toTry { new IllegalStateException("No generic unique access trait exists for a combined class") }
						.flatMap { genericUniqueAccessTraitRef =>
							parentRefs.genericManyAccessTrait
								.toTry { new IllegalStateException("No generic access trait exists for a combined class") }
								.flatMap { genericManyAccessTraitRef =>
									AccessWriter.writeComboAccessPoints(combination, genericUniqueAccessTraitRef,
										genericManyAccessTraitRef, combinedRefs.combined, comboFactoryRef,
										parentRefs.dbModel, childRefs.dbModel)
								}
						}
				}
		}
	}
}
