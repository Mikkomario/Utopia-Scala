package utopia.vault.coder.controller.writer.database

import utopia.flow.time.Today
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, DbProperty, Name, NamingRules, ProjectSetup}
import utopia.vault.coder.model.datatype.PropertyType
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Text}
import utopia.vault.coder.model.datatype.PropertyType.ClassReference

import java.io.PrintWriter
import java.nio.file.Path
import scala.annotation.tailrec
import scala.io.Codec
import scala.util.Success

/**
  * Used for converting class data into SQL
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object SqlWriter
{
	/**
	  * Writes the SQL document for the specified classes
	  * @param dbName Name of the database to use (optional)
	  * @param classes    Classes to write
	  * @param targetPath Path to which write the sql document
	  * @param codec      Implicit codec used when writing
	  * @return Target path. Failure if writing failed.
	  */
	def apply(dbName: Option[String], classes: Seq[Class], targetPath: Path)
	         (implicit codec: Codec, setup: ProjectSetup, naming: NamingRules) =
	{
		// Doesn't write anything if no classes are included
		if (classes.nonEmpty) {
			// Writes the table declarations in an order that attempts to make sure foreign keys are respected
			// (referenced tables are written before referencing tables)
			val allClasses = classes ++ classes.flatMap { _.descriptionLinkClass }
			val classesByTableName = allClasses.map { c => c.tableName -> c }.toMap
			val references = classesByTableName.map { case (tableName, c) =>
				val refs = c.properties.flatMap { _.dataType match {
					case ClassReference(referencedTableName, _, _) =>
						// References to the class' own table are ignored
						Some(referencedTableName.tableName).filterNot { _ == tableName }
					case _ => None
				} }
				c.tableName -> refs.toSet
			}
			// Forms the table initials, also
			val initials = initialsFrom(references.flatMap { case (tableName, refs) => refs + tableName }.toSet)
			targetPath.writeUsing { writer =>
				// Writes the header
				writer.println("-- ")
				writer.println(s"-- Database structure for ${setup.dbModuleName} models")
				setup.version.foreach { v => writer.println(s"-- Version: $v") }
				writer.println(s"-- Last generated: ${Today.toString}")
				writer.println("--")
				
				// Writes the database introduction, if needed
				dbName.foreach { dbName =>
					writer.println()
					writer.println(s"CREATE DATABASE IF NOT EXISTS `$dbName` ")
					writer.println("\tDEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;")
					writer.println(s"USE `$dbName`;")
				}
				
				// Groups the classes by package and writes them
				writeClasses(writer, initials, classesByTableName.groupBy { _._2.packageName }, references)
			}
		}
		else
			Success(targetPath)
	}
	
	// classesByPackageAndTableName: first key is package name (header) and second key is table name
	// references: Keys and values are both table names
	@tailrec
	private def writeClasses(writer: PrintWriter, initialsMap: Map[String, String],
	                          classesByPackageAndTableName: Map[String, Map[String, Class]],
	                          references: Map[String, Set[String]])
	                        (implicit setup: ProjectSetup, naming: NamingRules): Unit =
	{
		// Finds the classes which don't make any references to other remaining classes
		val remainingTableNames = classesByPackageAndTableName.flatMap { _._2.keys }.toSet
		val notReferencingTableNames = remainingTableNames
			.filterNot { tableName =>
				references(tableName)
					.exists { referencedTableName => remainingTableNames.contains(referencedTableName) }
			}
		// Case: All classes are referenced at least once (indicates a cyclic loop) => Writes them in alphabetical order
		if (notReferencingTableNames.isEmpty) {
			writer.println("\n-- WARNING: Following classes contain a cyclic loop\n")
			classesByPackageAndTableName.valuesIterator.flatMap { _.valuesIterator }.toVector.sortBy { _.name.singular }
				.foreach { writeClass(writer, _, initialsMap) }
		}
		// Case: There are some classes which don't reference remaining classes => writes those
		else {
			// Writes a single package, including as many classes as possible
			// Prefers packages which can be finished off, also preferring larger class sets
			// Package name -> (currently writeable classes, classes which are dependent from other remaining packages)
			val packagesWithInfo = classesByPackageAndTableName.map { case (packageName, classesByTableName) =>
				val packageClassTables = classesByTableName.keySet
				// Writeable = Class only makes references inside this package
				// Dependent = Class makes references to other remaining packages
				val (writeableClasses, dependentClasses) = classesByTableName.divideBy { case (tableName, _) =>
					references.get(tableName)
						.exists { refs => ((refs & remainingTableNames) -- packageClassTables).nonEmpty }
				}
				packageName -> (writeableClasses, dependentClasses)
			}
			// Finds the next package to target and starts writing classes within that package
			val (packageName, (writeableClasses, remainingPackageClasses)) = packagesWithInfo
				.bestMatch(Vector(_._2._2.isEmpty)).maxBy { _._2._1.size }
			val packageHeader = Name.interpret(packageName, CamelCase.lower).to(Text.allCapitalized).singular
			writer.println(s"\n--\t$packageHeader\t${"-" * 10}\n")
			val allRemainingPackageClasses = writePossibleClasses(writer, initialsMap, writeableClasses, references) ++
				remainingPackageClasses
			// Prepares the next recursive iteration
			val remainingClasses = {
				if (allRemainingPackageClasses.isEmpty)
					classesByPackageAndTableName - packageName
				else
					classesByPackageAndTableName + (packageName -> allRemainingPackageClasses)
			}
			if (remainingClasses.nonEmpty)
				writeClasses(writer, initialsMap, remainingClasses, references)
		}
	}
	
	@tailrec
	private def writePossibleClasses(writer: PrintWriter, initialsMap: Map[String, String],
	                                 classesByTableName: Map[String, Class], references: Map[String, Set[String]])
	                                (implicit setup: ProjectSetup, naming: NamingRules): Map[String, Class] =
	{
		// Finds the classes which don't make any references to other remaining classes
		val remainingTableNames = classesByTableName.keySet
		val notReferencingTableNames = remainingTableNames
			.filterNot { tableName => references(tableName)
				.exists { referencedTableName => remainingTableNames.contains(referencedTableName) } }
		// Case: All classes make at least once reference => sends them back to the original method caller
		if (notReferencingTableNames.isEmpty)
			classesByTableName
		// Case: There are some classes which don't reference remaining classes => writes those
		else {
			// Writes the classes in alphabetical order
			notReferencingTableNames.toVector.sorted
				.foreach { table => writeClass(writer, classesByTableName(table), initialsMap) }
			// Continues recursively. Returns the final group of remaining classes.
			val remainingClassesByTableName = classesByTableName -- notReferencingTableNames
			if (remainingClassesByTableName.nonEmpty)
				writePossibleClasses(writer, initialsMap, remainingClassesByTableName, references)
			else
				remainingClassesByTableName
		}
	}
	
	private def writeClass(writer: PrintWriter, classToWrite: Class, initialsMap: Map[String, String])
	                      (implicit setup: ProjectSetup, naming: NamingRules): Unit =
	{
		val tableName = classToWrite.tableName
		lazy val classInitials = initialsMap(tableName)
		def prefixColumn(column: DbProperty, parentType: PropertyType): String =
			prefixColumnName(column.columnName, parentType match {
				case ClassReference(table, _, _) => Some(table.tableName)
				case _ => None
			})
		def prefixColumnName(colName: String, referredTableName: => Option[String] = None): String = {
			if (setup.prefixSqlProperties) {
				referredTableName.flatMap(initialsMap.get) match {
					case Some(refInitials) => s"${classInitials}_${refInitials}_$colName"
					case None => classInitials + "_" + colName
				}
			}
			else
				colName
		}
		val idName = prefixColumnName(classToWrite.idName.columnName)
		// [(Property -> [(DbProperty -> Full Column Name)])]
		val namedProps = classToWrite.properties
			.map { prop => prop -> prop.dbProperties.map { dbProp => dbProp -> prefixColumn(dbProp, prop.dataType) } }
		val columns = namedProps.flatMap { _._2 }
		
		classToWrite.description.notEmpty.foreach { desc => writer.println(s"-- $desc") }
		// Writes property documentation
		val maxColumnNameLength: Int = columns.map { _._2.length }.maxOption.getOrElse(0)
		namedProps.foreach { case (prop, columns) =>
			if (prop.description.nonEmpty) {
				// If spans multiple columns, introduces all of them with the same description
				val name = {
					if (columns.size == 1)
						columns.head._2
					else
						s"${prop.name} (${columns.map { _._2 }.mkString(", ")})"
				}
				val propIntroduction = (name + ":").padTo(maxColumnNameLength + 1, ' ')
				writer.println(s"-- $propIntroduction ${ prop.description }")
			}
		}
		// Writes the table
		writer.println(s"CREATE TABLE `$tableName`(")
		val idBase = s"\t`$idName` ${ classToWrite.idType.sqlType.toSql } PRIMARY KEY AUTO_INCREMENT"
		if (columns.isEmpty)
			writer.println(idBase)
		else {
			writer.println(idBase + ", ")
			
			val propertyDeclarations = columns.map { case (prop, name) =>
				val defaultPart = prop.default.mapIfNotEmpty { " DEFAULT " + _ }
				s"`$name` ${ prop.sqlType.baseTypeSql }${ prop.sqlType.notNullPart }$defaultPart"
			}
			val comboIndexColumnNames = classToWrite.comboIndexColumnNames.map { _.map { prefixColumnName(_) } }
			val firstComboIndexColumns = comboIndexColumnNames.filter { _.size > 1 }.map { _.head }.toSet
			val individualIndexDeclarations = columns
				.filter { case (prop, name) => prop.isIndexed && !firstComboIndexColumns.contains(name) }
				.map { case (_, name) => s"INDEX ${ classInitials }_${ name }_idx (`$name`)" }
			val comboIndexDeclarations = comboIndexColumnNames.filter { _.size > 1 }
				.zipWithIndex.map { case (colNames, index) =>
				s"INDEX ${ classInitials }_combo_${ index + 1 }_idx (${ colNames.mkString(", ") })"
			}
			val foreignKeyDeclarations = namedProps.flatMap { case (prop, columns) =>
				prop.dataType match {
					case ClassReference(rawReferencedTableName, rawColumnName, referenceType) =>
						val refTableName = rawReferencedTableName.tableName
						val refInitials = initialsMap(refTableName)
						val refColumnName = {
							val base = rawColumnName.columnName
							if (setup.prefixSqlProperties)
								refInitials + "_" + base
							else
								base
						}
						val columnName = columns.headOption match {
							case Some((_, name)) => name
							case None => prop.name.columnName
						}
						val constraintNameBase = {
							val nameWithoutId = columnName.replace("_id", "")
							val base = {
								if (setup.prefixSqlProperties)
									nameWithoutId
								else
									s"${ classInitials }_${ refInitials }_$nameWithoutId"
							}
							base + "_ref"
						}
						Some(s"CONSTRAINT ${ constraintNameBase }_fk FOREIGN KEY ${
							constraintNameBase }_idx ($columnName) REFERENCES `$refTableName`(`$refColumnName`) ON DELETE ${
							if (referenceType.sqlConversions.forall { _.target.isNullable }) "SET NULL" else "CASCADE"
						}")
					case _ => None
				}
			}
			
			val allDeclarations = propertyDeclarations ++ individualIndexDeclarations ++ comboIndexDeclarations ++
				foreignKeyDeclarations
			allDeclarations.dropRight(1).foreach { line => writer.println(s"\t$line, ") }
			writer.println("\t" + allDeclarations.last)
		}
		
		writer.println(")Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;")
		writer.println()
	}
	
	private def initialsFrom(tableNames: Iterable[String], charsToTake: Int = 1): Map[String, String] =
	{
		// Generates initials
		val namePairs = tableNames.map { tableName => tableName -> initialsFrom(tableName, charsToTake) }
		val nameMap = namePairs.toMap
		// Checks for duplicates
		val reverseMap = namePairs.map { case (tableName, initial) => initial -> tableName }.toVector.asMultiMap
		val duplicates = reverseMap.filter { case (_, tableNames) => tableNames.size > 1 }
			.valuesIterator.toVector.flatten
		// Uses recursion to resolve the duplicates, if necessary
		if (duplicates.isEmpty)
			nameMap
		else
			nameMap ++ initialsFrom(duplicates, charsToTake + 1)
	}
	
	private def initialsFrom(tableName: String, charsToTake: Int): String =
	{
		if (charsToTake >= tableName.length)
			tableName
		else
			tableName.split("_").map { _.take(charsToTake) }.mkString
	}
}
