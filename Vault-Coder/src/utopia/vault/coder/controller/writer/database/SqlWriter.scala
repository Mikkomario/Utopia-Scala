package utopia.vault.coder.controller.writer.database

import utopia.flow.time.Today
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, NamingRules, ProjectSetup, Property}
import utopia.vault.coder.model.enumeration.PropertyType.ClassReference

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
	// Package name > reference count > class name
	private lazy val classOrdering = CombinedOrdering[(Class, Int)](
		Ordering.by[(Class, Int), String] { _._1.packageName },
		Ordering.by[(Class, Int), Int] { _._2 },
		Ordering.by[(Class, Int), String] { _._1.name.singular }
	)
	
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
			val references = allClasses.map { c =>
				val refs = c.properties.flatMap { _.dataType match {
					case ClassReference(referencedTableName, _, _, _) => Some(referencedTableName.tableName)
					case _ => None
				} }
				c.tableName -> refs.toSet
			}.toMap
			// Forms the table initials, also
			val initials = initialsFrom(references.flatMap { case (tableName, refs) => refs + tableName }.toSet)
			targetPath.writeUsing { writer =>
				// Writes the header
				writer.println("-- ")
				writer.println(s"-- Database structure for ${setup.dbModuleName} models")
				setup.version.foreach { v => writer.println(s"-- Version: $v") }
				writer.println(s"-- Last generated: ${Today.toString}")
				writer.println("--")
				writer.println()
				
				// Writes the database introduction, if needed
				dbName.foreach { dbName =>
					writer.println(s"CREATE DATABASE IF NOT EXISTS `$dbName` ")
					writer.println("\tDEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;")
					writer.println(s"USE `$dbName`;")
					writer.println()
				}
				
				// Writes the class declarations in order
				writeClasses(writer, initials, classesByTableName, references)
			}
		}
		else
			Success(targetPath)
	}
	
	@tailrec
	private def writeClasses(writer: PrintWriter, initialsMap: Map[String, String],
	                         classesByTableName: Map[String, Class], references: Map[String, Set[String]])
	                        (implicit setup: ProjectSetup, naming: NamingRules): Unit =
	{
		// Finds the classes which don't make any references to other remaining classes
		val remainingTableNames = classesByTableName.keySet
		val notReferencingTableNames = remainingTableNames
			.filterNot { tableName => references(tableName)
				.exists { referencedTableName => remainingTableNames.contains(referencedTableName) } }
		// Case: All classes are referenced at least once (indicates a cyclic loop) => Writes them in alphabetical order
		if (notReferencingTableNames.isEmpty)
			classesByTableName.valuesIterator.toVector.sortBy { _.name.singular }
				.foreach { writeClass(writer, _, initialsMap) }
		// Case: There are some classes which don't reference remaining classes => writes those
		else
		{
			// Writes the classes in order of package name > reference count > alphabetical order
			notReferencingTableNames.toVector
				.map { table => classesByTableName(table) -> references(table).size }.sorted(classOrdering)
				.foreach { case (classToWrite, _) => writeClass(writer, classToWrite, initialsMap) }
			// Continues recursively as long as classes remain
			val remainingClassesByTableName = classesByTableName -- notReferencingTableNames
			if (remainingClassesByTableName.nonEmpty)
				writeClasses(writer, initialsMap, remainingClassesByTableName, references)
		}
	}
	
	private def writeClass(writer: PrintWriter, classToWrite: Class, initialsMap: Map[String, String])
	                      (implicit setup: ProjectSetup, naming: NamingRules): Unit =
	{
		val tableName = classToWrite.tableName
		lazy val classInitials = initialsMap(tableName)
		def prefixColumn(column: Property): String = prefixColumnName(column.columnName, column.dataType match {
			case ClassReference(table, _, _, _) => Some(table.tableName)
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
		val namedProps = classToWrite.properties.map { prop => prop -> prefixColumn(prop) }
		
		classToWrite.description.notEmpty.foreach { desc => writer.println(s"-- $desc") }
		// Writes property documentation
		val maxPropNameLength = namedProps.map { _._2.length }.maxOption.getOrElse(0)
		namedProps.foreach { case (prop, name) =>
			if (prop.description.nonEmpty || prop.useDescription.nonEmpty) {
				val propIntroduction = (name + ":").padTo(maxPropNameLength + 1, ' ')
				if (prop.description.nonEmpty) {
					writer.println(s"-- $propIntroduction ${ prop.description }")
					if (prop.useDescription.nonEmpty)
						writer.println(s"-- \t${ prop.useDescription }")
				}
				else
					writer.println(s"-- $propIntroduction ${ prop.useDescription }")
			}
		}
		// Writes the table
		writer.println(s"CREATE TABLE `$tableName`(")
		val idBase = s"\t$idName ${ classToWrite.idType.toSql } PRIMARY KEY AUTO_INCREMENT"
		if (namedProps.isEmpty)
			writer.println(idBase)
		else {
			writer.println(idBase + ", ")
			
			val propertyDeclarations = namedProps.map { case (prop, name) =>
				val defaultPart = prop.sqlDefault.notEmpty match {
					case Some(default) => s" DEFAULT $default"
					case None => ""
				}
				s"`$name` ${ prop.dataType.toSql }$defaultPart"
			}
			val comboIndexColumnNames = classToWrite.comboIndexColumnNames.map { _.map { prefixColumnName(_) } }
			val firstComboIndexColumns = comboIndexColumnNames.filter { _.size > 1 }.map { _.head }.toSet
			val individualIndexDeclarations = namedProps
				.filter { case (prop, name) => prop.isIndexed && !firstComboIndexColumns.contains(name) }
				.map { case (_, name) => s"INDEX ${ classInitials }_${ name }_idx (`$name`)" }
			val comboIndexDeclarations = comboIndexColumnNames.filter { _.size > 1 }
				.zipWithIndex.map { case (colNames, index) =>
				s"INDEX ${ classInitials }_combo_${ index + 1 }_idx (${ colNames.mkString(", ") })"
			}
			val foreignKeyDeclarations = namedProps.flatMap { case (prop, name) =>
				prop.dataType match {
					case ClassReference(rawReferencedTableName, rawColumnName, _, isNullable) =>
						val refTableName = rawReferencedTableName.tableName
						val refInitials = initialsMap(refTableName)
						val refColumnName = {
							val base = rawColumnName.columnName
							if (setup.prefixSqlProperties)
								refInitials + "_" + base
							else
								base
						}
						val constraintNameBase = {
							val nameWithoutId = name.replace("_id", "")
							val base = {
								if (setup.prefixSqlProperties)
									nameWithoutId
								else
									s"${ classInitials }_${ refInitials }_$nameWithoutId"
							}
							base + "_ref"
						}
						Some(s"CONSTRAINT ${ constraintNameBase }_fk FOREIGN KEY ${
							constraintNameBase }_idx ($name) REFERENCES `$refTableName`(`$refColumnName`) ON DELETE ${
							if (isNullable) "SET NULL" else "CASCADE"
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
