package utopia.vault.coder.controller.writer.database

import utopia.flow.time.Today
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.enumeration.PropertyType.ClassReference

import java.io.PrintWriter
import java.nio.file.Path
import scala.annotation.tailrec
import scala.io.Codec

/**
  * Used for converting class data into SQL
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object SqlWriter
{
	private lazy val classOrdering = CombinedOrdering[(Class, Int)](
		Ordering.by[(Class, Int), Int] { _._2 }, Ordering.by[(Class, Int), String] { _._1.name.singular }
	)
	
	/**
	  * Writes the SQL document for the specified classes
	  * @param classes    Classes to write
	  * @param targetPath Path to which write the sql document
	  * @param codec      Implicit codec used when writing
	  * @return Target path. Failure if writing failed.
	  */
	def apply(classes: Seq[Class], targetPath: Path)(implicit codec: Codec, setup: ProjectSetup) =
	{
		// Writes the table declarations in an order that attempts to make sure foreign keys are respected
		// (referenced tables are written before referencing tables)
		val allClasses = classes ++ classes.flatMap { _.descriptionLinkClass }
		val classesByTableName = allClasses.map { c => c.tableName -> c }.toMap
		val references = allClasses.map { c =>
			val refs = c.properties.flatMap { _.dataType match {
				case ClassReference(referencedTableName, _, _) => Some(referencedTableName)
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
			
			// Writes the class declarations in order
			writeClasses(writer, initials, classesByTableName, references)
		}
	}
	
	@tailrec
	private def writeClasses(writer: PrintWriter, initialsMap: Map[String, String],
	                         classesByTableName: Map[String, Class], references: Map[String, Set[String]]): Unit =
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
			// Writes the classes in reference count + alphabetical order
			notReferencingTableNames.toVector
				.map { table => classesByTableName(table) -> references(table).size }.sorted(classOrdering)
				.foreach { case (classToWrite, _) => writeClass(writer, classToWrite, initialsMap) }
			// Continues recursively as long as classes remain
			val remainingClassesByTableName = classesByTableName -- notReferencingTableNames
			if (remainingClassesByTableName.nonEmpty)
				writeClasses(writer, initialsMap, remainingClassesByTableName, references)
		}
	}
	
	private def writeClass(writer: PrintWriter, classToWrite: Class, initialsMap: Map[String, String]): Unit =
	{
		classToWrite.description.notEmpty.foreach { desc => writer.println(s"-- $desc") }
		// Writes property documentation
		val maxPropNameLength = classToWrite.properties.map { _.name.singular.length }.maxOption.getOrElse(0)
		classToWrite.properties.foreach { prop =>
			if (prop.description.nonEmpty || prop.useDescription.nonEmpty) {
				val propIntroduction = (prop.columnName + ":").padTo(maxPropNameLength + 1, ' ')
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
		val classInitials = initialsMap(classToWrite.tableName)
		writer.println(s"CREATE TABLE `${ classToWrite.tableName }`(")
		val idBase = s"\tid ${ classToWrite.idType.toSql } PRIMARY KEY AUTO_INCREMENT"
		if (classToWrite.properties.isEmpty)
			writer.println(idBase)
		else {
			writer.println(idBase + ", ")
			
			val propertyDeclarations = classToWrite.properties.map { prop =>
				val defaultPart = prop.sqlDefault.notEmpty match {
					case Some(default) => s" DEFAULT $default"
					case None => ""
				}
				s"`${ prop.columnName }` ${ prop.dataType.toSql }$defaultPart"
			}
			val firstComboIndexColumns = classToWrite.comboIndexColumnNames.filter { _.size > 1 }.map { _.head }.toSet
			val individualIndexDeclarations = classToWrite.properties
				.filter { prop => prop.isIndexed && !firstComboIndexColumns.contains(prop.columnName) }
				.map { prop => s"INDEX ${ classInitials }_${ prop.columnName }_idx (`${ prop.columnName }`)" }
			val comboIndexDeclarations = classToWrite.comboIndexColumnNames.filter { _.size > 1 }
				.zipWithIndex.map { case (colNames, index) =>
				s"INDEX ${ classInitials }_combo_${ index + 1 }_idx (${ colNames.mkString(", ") })"
			}
			val foreignKeyDeclarations = classToWrite.properties.flatMap { prop =>
				prop.dataType match {
					case ClassReference(referencedTableName, _, isNullable) =>
						val constraintNameBase = s"${ classInitials }_${ initialsMap(referencedTableName) }_${
							prop.columnName.replace("_id", "")
						}_ref"
						Some(s"CONSTRAINT ${ constraintNameBase }_fk FOREIGN KEY ${ constraintNameBase }_idx (${
							prop.columnName
						}) REFERENCES `$referencedTableName`(id) ON DELETE ${
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
