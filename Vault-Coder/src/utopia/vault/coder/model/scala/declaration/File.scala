package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.template.CodeConvertible

import scala.collection.immutable.VectorBuilder

object File
{
	/**
	  * Creates a new file
	  * @param packagePath Package for the contents of this file
	  * @param firstDeclaration First instance declaration
	  * @param moreDeclarations More instance declarations
	  * @return A new file containing the specified declarations
	  */
	def apply(packagePath: String, firstDeclaration: InstanceDeclaration,
	          moreDeclarations: InstanceDeclaration*): File =
		apply(packagePath, firstDeclaration +: moreDeclarations.toVector)
}

/**
  * Represents a scala file
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
case class File(packagePath: String, declarations: Vector[InstanceDeclaration])
	extends CodeConvertible
{
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		
		// Writes the package declaration
		builder += s"package $packagePath"
		builder += ""
		
		// Writes the imports
		val imports = declarations.flatMap { _.references }.toSet.groupMap[String, String] { _.parentPath } { _.target }
		imports.keys.toVector.sorted.foreach { path =>
			val targets = imports(path)
			val finalPart =
			{
				if (targets.contains("_"))
					"_"
				else if (targets.size == 1)
					targets.head
				else
					s"{ ${targets.toVector.sorted.mkString(", ") } }"
			}
			builder += s"import $path.$finalPart"
		}
		if (imports.nonEmpty)
			builder += ""
		
		// Writes the objects, then classes
		declarations.foreach { declaration =>
			builder ++= declaration.toCodeLines
			builder += ""
		}
		
		builder.result()
	}
}
