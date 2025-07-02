package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Row, Table, TableColumn}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType

/**
  * Common trait for interfaces that implement [[TargetingManyRowsLike]] by wrapping another such instance.
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait TargetingManyRowsWrapper[T <: TargetingManyRowsLike[O, T, OT], OT, O, +A, +Repr, +One]
	extends TargetingManyWrapper[T, OT, O, A, Repr, One] with TargetingManyRowsLike[A, Repr, One]
{
	// IMPLEMENTED  ------------------------
	
	override def take(n: Int): Repr = mapWrapped { _.take(n) }
	override def drop(n: Int): Repr = mapWrapped { _.drop(n) }
	override def slice(range: HasInclusiveEnds[Int]): Repr = mapWrapped { _.slice(range) }
	
	override def stream[B](f: Iterator[A] => B)(implicit connection: Connection): B =
		wrapped.stream[B] { items => f(items.map(mapResult)) }
	
	override def pullWith[B](column: TableColumn)(map: Value => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWith(column)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWith[B](columns: Seq[TableColumn])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWith(columns)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWithMany[B](column: TableColumn)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWithMany(column)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWithMany[B](columns: Seq[TableColumn])(map: Seq[Seq[Value]] => B)
	                            (implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWithMany(columns)(map).map { case (a, b) => mapResult(a) -> b }
	
	override def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
	                         joinType: JoinType)(f: Iterator[(A, Row)] => Seq[B]) =
		wrapped.extendTo(tables, exclusiveColumns, bridgingJoins, joinType) { items =>
			f(items.map { case (a, row) => mapResult(a) -> row })
		}
	override def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
	                             joinType: JoinType)
	                            (f: Iterator[(A, Seq[Row])] => Seq[B]) =
		wrapped.extendToMany(tables, exclusiveColumns, bridgingJoins, joinType) { items =>
			f(items.map { case (a, rows) => mapResult(a) -> rows })
		}
}
