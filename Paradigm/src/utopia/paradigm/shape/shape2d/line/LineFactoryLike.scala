package utopia.paradigm.shape.shape2d.line

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Combinable
import utopia.paradigm.shape.template.{DimensionalFactory, HasDimensions}

import scala.collection.immutable.VectorBuilder

/**
  * Common trait for factories used for constructing 2-dimensional lines
  * @author Mikko Hilpinen
  * @since 27.8.2023
  * @tparam D Type of dimensions used at the line endpoints
  * @tparam P Type of line endpoints
  * @tparam L Type of the constructed lines
  */
trait LineFactoryLike[-D, P <: Combinable[HasDimensions[D], P], +L]
{
    // ABSTRACT -----------------------------
    
    /**
      * @return A factory used for constructing new line endpoints
      */
    protected def pointFactory: DimensionalFactory[D, P]
    
    /**
      * @param ends The two ends of a line
      * @return A line between those two points
      */
    def apply(ends: Pair[P]): L
    
    
    // COMPUTED   ---------------------------
    
    /**
      * A line between (0,0) and (0,0)
      */
    def zero = apply(Pair.twice(pointFactory.empty))
    
    
    // OTHER METHODS    ---------------------
    
    /**
      * @param start Line start point
      * @param end Line end point
      * @return A new line
      */
    def apply(start: P, end: P): L = apply(Pair(start, end))
    
    /**
     * Creates a new line from position and vector combo
     * @param start The starting position of the line
     * @param vector The vector portion of the line
     * @return A line with the provided position and vector part
     */
    def fromVector(start: P, vector: HasDimensions[D]) = apply(start, start + vector)
    
    /**
     * Creates a set of edges from the specified vertices.
      * The vertices are iterated in order and an edge is placed between each consecutive vertex.
     * @param vertices An ordered collection of vertices
     * @param close Should the shape be closed by connecting the last and the first vertex.
      *              Defaults to true
     * @return The edges that were formed between the vertices. Empty if there were less than 2 vertices
     */
    def edgesForVertices(vertices: Seq[P], close: Boolean = true) = {
        if (vertices.size < 2)
            Vector()
        else {
            val buffer = new VectorBuilder[L]()
            vertices.iterator.paired.foreach { buffer += apply(_) }
            if (close)
                buffer += apply(vertices.last, vertices.head)
            
            buffer.result()
        }
    }
}

