package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Row, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType
import utopia.vault.sql.JoinType.Inner

object TargetingManyRowsLike
{
	// EXTENSIONS   --------------------------
	
	implicit class RecursiveTargetingManyRows[T <: TargetingManyRowsLike[A, T, _], +A](val t: T) extends AnyVal
	{
		def slicesIterator(sliceLength: Int)(implicit connection: Connection) =
			Iterator.iterate(0) { _ + sliceLength }
				.map { start => t.slice(NumericSpan(start, start + sliceLength - 1)).pull }
				.takeTo { _.hasSize < sliceLength }
		
		def slicedIterator(sliceLength: Int)(implicit connection: Connection) =
			slicesIterator(sliceLength).flatten
	}
}

/**
  * Common trait for extendable & filterable access points which fetch multiple row-specific items with each query
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait TargetingManyRowsLike[+A, +Repr, +One] extends TargetingManyLike[A, Repr, One]
{
	// ABSTRACT --------------------------
	
	def take(n: Int): Repr
	def drop(n: Int): Repr
	def slice(range: HasInclusiveEnds[Int]): Repr
	
	// NB: These won't join
	def pullWith[B](column: Column)(map: Value => B)(implicit connection: Connection): Seq[(A, B)]
	def pullWith[B](columns: Seq[Column])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)]
	def pullWithMany[B](column: Column)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)]
	def pullWithMany[B](columns: Seq[Column])(map: Seq[Seq[Value]] => B)(implicit connection: Connection): Seq[(A, B)]
	
	def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridgingJoins: Seq[Joinable] = Empty,
	                joinType: JoinType = Inner)
	               (f: Seq[(A, Row)] => Seq[B]): TargetingMany[B]
	def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridgingJoins: Seq[Joinable] = Empty,
	                    joinType: JoinType = Inner)
	                   (f: Seq[(A, Seq[Row])] => Seq[B]): TargetingMany[B]
	
	
	// OTHER    --------------------------
	
	def pullWith[B](firstColumn: Column, secondColumn: Column, moreColumns: Column*)(map: Seq[Value] => B)
	               (implicit connection: Connection): Seq[(A, B)] =
		pullWith[B](Pair(firstColumn, secondColumn) ++ moreColumns)(map)
	def pullWithMany[B](firstColumn: Column, secondColumn: Column, moreColumns: Column*)(map: Seq[Seq[Value]] => B)
	                   (implicit connection: Connection): Seq[(A, B)] =
		pullWithMany[B](Pair(firstColumn, secondColumn) ++ moreColumns)(map)
		
	def groupBy[B](column: Column)(map: Value => B)(implicit connection: Connection) =
		pullWith(column)(map).groupMap { _._2 } { _._1 }
	def groupBy[B](columns: Seq[Column])(map: Seq[Value] => B)(implicit connection: Connection) =
		pullWith(columns)(map).groupMap { _._2 } { _._1 }
	def groupBy[B](firstColumn: Column, secondColumn: Column, moreColumns: Column*)(map: Seq[Value] => B)
	              (implicit connection: Connection): Map[B, Seq[A]] =
		groupBy(Pair(firstColumn, secondColumn) ++ moreColumns)(map)
}
