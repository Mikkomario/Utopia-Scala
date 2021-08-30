package utopia.citadel.coder.model.scala

/**
  * Represents one or more lines of scala code
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Code(lines: Vector[String], references: Set[Reference] = Set()) extends Referencing
