package utopia.genesis.shape.shape2D

/**
  * A 2D shape that consists of 4 points
  * @param topLeft The top left point
  * @param top Vector from the top left point to the top right point
  * @param left Vector from the top left point to tbe bottom left point
  */
case class Parallelogram(topLeft: Point, top: Vector2D, left: Vector2D) extends Parallelogramic
{
	override def toString = s"Origin: $topLeft, Top Side: $top, Left Side: $left"
}