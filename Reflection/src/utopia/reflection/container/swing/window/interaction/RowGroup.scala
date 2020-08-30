package utopia.reflection.container.swing.window.interaction

object RowGroup
{
	/**
	  * @param rows Input rows
	  * @return A group based on those rows
	  */
	def apply[R](rows: Vector[R]) = new RowGroup(rows)
	
	/**
	  * @param first First input row
	  * @param second Second input row
	  * @param more More input rows
	  * @return A group based on those rows
	  */
	def apply[R](first: R, second: R, more: R*): RowGroup[R] =
		apply(Vector(first, second) ++ more)
	
	/**
	  * @param row The only input row for this group
	  * @return A group that consists only of the specified row
	  */
	def singleRow[R](row: R) = apply(Vector(row))
}

/**
  * Groups multiple input rows into a single group entity
  * @author Mikko Hilpinen
  * @since 7.5.2020, v1.2
  * @param rows Input rows that form this group
  * @tparam Row Row type
  */
class RowGroup[+Row](val rows: Vector[Row])
{
	// COMPUTED	----------------------
	
	/**
	  * @return Whether this group is empty
	  */
	def isEmpty = rows.isEmpty
	
	/**
	  * @return Whether this group is not empty
	  */
	def nonEmpty = rows.nonEmpty
	
	/**
	  * @return Whether this group consists of a single row
	  */
	def isSingleRow = rows.size == 1
	
	/**
	  * @return Whether this group contains multiple rows
	  */
	def isMultipleRows = rows.size > 1
	
	
	// OTHER	----------------------
	
	/**
	  * Maps this row group contents
	  * @param f A mapping function
	  * @tparam B Map result type
	  * @return Mapped row group
	  */
	def map[B](f: Row => B) = RowGroup(rows.map(f))
}
