package utopia.vault.coder.controller.writer

import utopia.vault.coder.model.enumeration.PropertyType.ClassReference
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.Class

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
	  * @param classes Classes to write
	  * @param targetPath Path to which write the sql document
	  * @param codec Implicit codec used when writing
	  * @return Target path. Failure if writing failed.
	  */
	def apply(classes: Seq[Class], targetPath: Path)(implicit codec: Codec) =
	{
		targetPath.writeUsing { writer =>
			classes.foreach { classToWrite =>
				classToWrite.description.notEmpty.foreach { desc => writer.println(s"-- $desc") }
				// Writes property documentation
				classToWrite.properties.foreach { prop =>
					if (prop.description.nonEmpty || prop.useDescription.nonEmpty)
					{
						if (prop.description.nonEmpty)
						{
							writer.println(s"-- ${prop.columnName}: ${prop.description}")
							if (prop.useDescription.nonEmpty)
								writer.println(s"-- \t${prop.useDescription}")
						}
						else
							writer.println(s"-- ${prop.columnName}: ${prop.useDescription}")
					}
				}
				// Writes the table
				val classInitials = initialsFrom(classToWrite.tableName)
				writer.println(s"CREATE TABLE ${classToWrite.tableName}(")
				val idBase = s"\tid ${classToWrite.idType.toSql} PRIMARY KEY AUTO_INCREMENT"
				if (classToWrite.properties.isEmpty)
					writer.println(idBase)
				else
				{
					writer.println(idBase + ", ")
					
					val propertyDeclarations = classToWrite.properties
						.map { prop => s"`${prop.columnName}` ${prop.dataType.toSql}" }
					val indexDeclarations = classToWrite.properties.filter { _.dataType.createsIndex }
						.map { prop => s"INDEX ${classInitials}_${prop.columnName}_idx (`${prop.columnName})`" }
					val foreignKeyDeclarations = classToWrite.properties.flatMap { prop =>
						prop.dataType match
						{
							case ClassReference(referencedTableName, _, isNullable) =>
								val constraintNameBase = s"${classInitials}_${initialsFrom(referencedTableName)}_${
									prop.columnName.replace("_id", "")}_ref"
								Some(s"CONSTRAINT ${constraintNameBase}_fk FOREIGN KEY ${constraintNameBase}_idx (${
									prop.columnName}) REFERENCES `$referencedTableName`(id) ON DELETE ${
									if (isNullable) "SET NULL" else "CASCADE"}")
							case _ => None
						}
					}
					
					val allDeclarations = propertyDeclarations ++ indexDeclarations ++ foreignKeyDeclarations
					allDeclarations.dropRight(1).foreach { line => writer.println(s"\t$line, ") }
					writer.println("\t" + allDeclarations.last)
				}
				
				writer.println(")Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;")
				writer.println()
			}
		}
	}
	
	private def initialsFrom(tableName: String) = tableName.split("_").flatMap { _.headOption }.mkString
}
