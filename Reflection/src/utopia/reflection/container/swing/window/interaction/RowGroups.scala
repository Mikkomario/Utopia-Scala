package utopia.reflection.container.swing.window.interaction

object RowGroups
{
	/**
	  * @param groups Input row groups
	  * @return A set of input rows based on those groups
	  */
	def apply[R](groups: Vector[RowGroup[R]]) = new RowGroups(groups)
	
	/**
	  * @param first First group
	  * @param second Second group
	  * @param more More groups
	  * @return A set of input rows based on those groups
	  */
	def apply[R](first: RowGroup[R], second: RowGroup[R], more: RowGroup[R]*): RowGroups[R] =
		apply(Vector(first, second) ++ more)
	
	/**
	  * @param group Only row group
	  * @return A set of input rows that only contains that group
	  */
	def singleGroup[R](group: RowGroup[R]) = apply(Vector(group))
	
	/**
	  * @param rows Input rows
	  * @return A set of input rows where the specified rows form a single group
	  */
	def singleGroup[R](rows: Vector[R]): RowGroups[R] = singleGroup(RowGroup(rows))
	
	/**
	  * @param first First row
	  * @param second Second row
	  * @param more More rows
	  * @return A set of input rows where the specified rows form a single group
	  */
	def singleGroup[R](first: R, second: R, more: R*): RowGroups[R] = singleGroup(Vector(first, second) ++ more)
	
	/**
	  * @param row Input row
	  * @return A set that only contains the specified input row
	  */
	def singleRow[R](row: R) = singleGroup(RowGroup.singleRow(row))
	
	/**
	  * @param rows Input rows
	  * @return A set of rows where each of the specified rows forms its own group
	  */
	def separateGroups[R](rows: Vector[R]) = apply(rows.map(RowGroup.singleRow))
	
	/**
	  * @param first First row
	  * @param second Second row
	  * @param more More rows
	  * @return A set of rows where each of the specified rows forms its own group
	  */
	def separateGroups[R](first: R, second: R, more: R*): RowGroups[R] = separateGroups(Vector(first, second) ++ more)
}

/**
  * Contains a hierarchical list of rows
  * @author Mikko Hilpinen
  * @since 7.5.2020, v1.2
  * @param groups Input row groups that form this main group
  * @tparam Row Type of rows in these groups
  */
class RowGroups[+Row](val groups: Vector[RowGroup[Row]])
{
	// COMPUTED	----------------------------
	
	/**
	  * All input rows within this set
	  */
	lazy val rows = groups.flatMap { _.rows }
	
	/**
	  * @return Whether this set of rows is completely empty
	  */
	def isEmpty = groups.forall { _.isEmpty }
	
	/**
	  * @return Whether this set contains any rows
	  */
	def nonEmpty = groups.exists { _.nonEmpty }
	
	/**
	  * @return Whether this set consists only of a single group
	  */
	def isSingleGroup = groups.size == 1
	
	/**
	  * @return Whether this set consists of multiple groups
	  */
	def isMultipleGroups = groups.size > 1
	
	/**
	  * @return Whether this set consists only of a single row
	  */
	def isSingleRow = isSingleGroup && groups.head.isSingleRow
	
	
	// OTHER	------------------------------
	
	/**
	  * Maps all row groups
	  * @param f A mapping function for groups
	  * @tparam B New row type
	  * @return A mapped version of these groups
	  */
	def mapGroups[B](f: RowGroup[Row] => RowGroup[B]) = new RowGroups(groups.map(f))
	
	/**
	  * Maps all rows in these groups
	  * @param f A mapping function for rows
	  * @tparam B New row type
	  * @return A mapped version of these groups
	  */
	def mapRows[B](f: Row => B) = mapGroups { _.map(f) }
}
