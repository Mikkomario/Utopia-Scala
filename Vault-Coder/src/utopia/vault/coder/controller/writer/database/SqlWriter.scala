package utopia.vault.coder.controller.writer.database

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.Class
import utopia.vault.coder.model.enumeration.PropertyType.ClassReference

import java.io.PrintWriter
import java.nio.file.Path
import scala.io.Codec

/**
  * Used for converting class data into SQL
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object SqlWriter
{
	/**
	  * Writes the SQL document for the specified classes
	  * @param classes    Classes to write
	  * @param targetPath Path to which write the sql document
	  * @param codec      Implicit codec used when writing
	  * @return Target path. Failure if writing failed.
	  */
	def apply(classes: Seq[Class], targetPath: Path)(implicit codec: Codec) =
	{
		// Forms the table initials first
		val initials = initialsFrom((classes.map { _.tableName } ++
			classes.flatMap { _.properties.flatMap { _.dataType match
			{
				case ClassReference(referencedTableName, _, _) => Some(referencedTableName)
				case _ => None
			} } }).toSet)
		// Writes the class declarations in order
		targetPath.writeUsing { writer => classes.foreach { writeClass(writer, _, initials) } }
	}
	
	private def writeClass(writer: PrintWriter, classToWrite: Class, initialsMap: Map[String, String]): Unit =
	{
		classToWrite.description.notEmpty.foreach { desc => writer.println(s"-- $desc") }
		// Writes property documentation
		classToWrite.properties.foreach { prop =>
			if (prop.description.nonEmpty || prop.useDescription.nonEmpty) {
				if (prop.description.nonEmpty) {
					writer.println(s"-- ${ prop.columnName }: ${ prop.description }")
					if (prop.useDescription.nonEmpty)
						writer.println(s"-- \t${ prop.useDescription }")
				}
				else
					writer.println(s"-- ${ prop.columnName }: ${ prop.useDescription }")
			}
		}
		// Writes the table
		val classInitials = initialsMap(classToWrite.tableName)
		writer.println(s"CREATE TABLE ${ classToWrite.tableName }(")
		val idBase = s"\tid ${ classToWrite.idType.toSql } PRIMARY KEY AUTO_INCREMENT"
		if (classToWrite.properties.isEmpty)
			writer.println(idBase)
		else {
			writer.println(idBase + ", ")
			
			val propertyDeclarations = classToWrite.properties
				.map { prop => s"`${ prop.columnName }` ${ prop.dataType.toSql }" }
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
		
		// If the class supports descriptions, writes a link class for those also
		classToWrite.descriptionLinkClass.foreach { writeClass(writer, _, initialsMap) }
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
