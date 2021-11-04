package utopia.vault.coder.model.merging

import utopia.vault.coder.model.scala.code.CodeLine

object MergeConflict
{
	/**
	  * @param text Text within this note
	  * @return A conflict that doesn't contain any code
	  */
	def note(text: String) = apply(description = text)
	
	/**
	  * @param read Read code line
	  * @param generated Generated code line
	  * @param description Description of the conflict (optional)
	  * @return A new merge conflict
	  */
	def line(read: String, generated: String, description: String = ""): MergeConflict =
		apply(Vector(CodeLine(read)), Vector(CodeLine(generated)), description)
}

/**
  * Represents a case where there is a conflict between two versions
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  * @param readVersion      Conflicting code read from a scala file
  * @param generatedVersion Conflicting generated code
  * @param description      Description of the merge conflict
  */
case class MergeConflict(readVersion: Vector[CodeLine] = Vector(), generatedVersion: Vector[CodeLine] = Vector(),
                         description: String = "")
