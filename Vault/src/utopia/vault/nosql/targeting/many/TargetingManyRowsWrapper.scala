package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

/**
  * Common trait for interfaces that implement [[TargetingManyRowsLike]] by wrapping another such instance.
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait TargetingManyRowsWrapper[T <: TargetingManyRowsLike[O, T], O, +A, +Repr]
	extends TargetingManyWrapper[T, O, A, Repr] with TargetingManyRowsLike[A, Repr]
{
	// IMPLEMENTED  ------------------------
	
	override def take(n: Int): Repr = mapWrapped { _.take(n) }
	override def drop(n: Int): Repr = mapWrapped { _.drop(n) }
	override def slice(range: HasInclusiveEnds[Int]): Repr = mapWrapped { _.slice(range) }
	
	override def pullWith[B](column: Column)(map: Value => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWith(column)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWith[B](columns: Seq[Column])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWith(columns)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWithMany[B](column: Column)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWithMany(column)(map).map { case (a, b) => mapResult(a) -> b }
	override def pullWithMany[B](columns: Seq[Column])(map: Seq[Seq[Value]] => B)
	                            (implicit connection: Connection): Seq[(A, B)] =
		wrapped.pullWithMany(columns)(map).map { case (a, b) => mapResult(a) -> b }
}
