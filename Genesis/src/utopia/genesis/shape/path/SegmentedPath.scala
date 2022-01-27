package utopia.genesis.shape.path

/**
  * A path that consists of predetermined value options. Notice that the start and end values are "positioned"
  * at the edges of this path, therefore having half the usual "space" / length when compared to other values.
  * @author Mikko Hilpinen
  * @since 16.9.2020, v2.4
  * @param values Values that are returned by this path
  * @throws IllegalArgumentException If length of values is smaller than 2
  */
case class SegmentedPath[+A](values: Seq[A]) extends Path[A]
{
	// ATTRIBUTES   -------------------------
	
	private val progressPerValue = 1.0 / (values.size - 1)
	
	
	// INITIAL CODE -------------------------
	
	if (values.size < 2)
		throw new IllegalArgumentException(s"SegmentedPath requires at least two input values (${values.size} were given)")
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Progress points that lie at the "center"/main area of each possible value
	  *         (starts with 0.0 and ends with 1.0)
	  */
	def centerProgressPoints = 0.0 +: (1 until values.size).map { i => progressPerValue * i } :+ 1.0
	
	/**
	  * @return Progress points where the value changes
	  */
	def thresholdPoints = (1 until values.size).map { i => progressPerValue * (i - 0.5) }
	
	
	// IMPLEMENTED  -------------------------
	
	override def start = values.head
	
	override def end = values.last
	
	override def apply(progress: Double) =
	{
		if (progress <= progressPerValue / 2.0)
			start
		else if (progress >= 1 - progressPerValue / 2.0)
			end
		else
			values(((progress - progressPerValue / 2.0) / progressPerValue).toInt + 1)
	}
}
