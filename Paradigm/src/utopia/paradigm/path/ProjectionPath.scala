package utopia.paradigm.path

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.vector.NumericVectorLike

/**
  * Common trait for paths that support vector projections (in double precision)
  * @author Mikko Hilpinen
  * @since 29.05.2024, v1.6
  */
trait ProjectionPath[+P] extends Path[P]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Axis formed by this path used in vector projections (i.e. when forming t and dt)
	  */
	def tAxis: Vector2D
	
	/**
	  * @return The t-axis position which matches the start of this path
	  */
	def t0: Double
	/**
	  * @return The length of this path along the t-axis.
	  *         NB: May be different from this path's actual length, if this path is not linear.
	  */
	def tLength: Double
	
	
	// OTHER    ------------------------
	
	/**
	  * @param p A point
	  * @return The t-axis position matching that point.
	  */
	def tFor(p: NumericVectorLike[_, _, _]) = p.scalarProjection(tAxis)
	/**
	  * @param p A point
	  * @return The t-axis progress within this path which matches that point.
	  *         E.g. Return value of 0 would match this path's start point and [[tLength]]
	  *         would match this path's end point.
	  *         Note: Returned dt value may be outside of this path's valid dt range.
	  * @see [[liftDtFor]]
	  */
	def dtFor(p: NumericVectorLike[_, _, _]) = tFor(p) - t0
	/**
	  * @param p A point
	  * @return The t-axis progress within this path which matches that point.
	  *         E.g. Return value of 0 would match this path's start point and [[tLength]]
	  *         would match this path's end point.
	  *         None if the specified point doesn't match a point on this path.
	  */
	def liftDtFor(p: NumericVectorLike[_, _, _]) =
		Some(dtFor(p)).filter { dt => dt >= 0 && dt <= tLength }
	/**
	  * @param p A point
	  * @return The t-axis progress on this path which most closely matches that point.
	  *         Limits to return values within this path.
	  */
	def capDtFor(p: NumericVectorLike[_, _, _]) = (dtFor(p) max 0) min tLength
	
	/**
	  * @param dt Distance traveled along this path,
	  *           where 0 matches the start of this path and [[tLength]] matches the end of this path.
	  * @return A point matching that travel distance.
	  *         Note: The returned point may lie outside of this path, if the specified travel distance is out-of-bounds.
	  * @see [[liftForDt]]
	  */
	def forDt(dt: Double) = apply(dt / tLength)
	/**
	  * @param dt Distance traveled along this path,
	  *           where 0 matches the start of this path and [[tLength]] matches the end of this path.
	  * @return A point matching that travel distance.
	  *         None if the specified travel distance is out-of-bounds.
	  */
	def liftForDt(dt: Double) = if (dt < 0 || dt > tLength) None else Some(forDt(dt))
	/**
	  * @param dt Distance traveled along this path
	  *           where 0 matches the start of this path and [[tLength]] matches the end of this path.
	  * @return A point on this path matching the specified travel distance.
	  *         If the specified travel distance is out-of-bounds, returns the start or the end of this path.
	  */
	def capForDt(dt: Double) = forDt((dt max 0) min tLength)
	
	/**
	  * @param t A t-axis position
	  * @return A point along this path which has the same t-axis position.
	  *         Note: Resulting point may lay outside of this path's range.
	  */
	def forT(t: Double) = forDt(t - t0)
	/**
	  * @param t A t-axis position
	  * @return A point along this path which has the same t-axis position.
	  *         None if there existed no matching point on this path
	  *         (i.e. if the specified t-axis position was out-of-bounds).
	  */
	def liftForT(t: Double) = liftForDt(t - t0)
	/**
	  * @param t A t-axis position
	  * @return A point on this path which has the closest matching t-axis position.
	  *         Limits to points on this path, meaning that the start or the end of this path may be returned
	  *         instead of a projected position.
	  */
	def capForT(t: Double) = capForDt(t - t0)
	
	/**
	  * @param p A point
	  * @return A point along this path that has the same t-axis position.
	  *         Note: The returned point may lie outside of this path.
	  * @see [[liftMatching]]
	  */
	def matching(p: NumericVectorLike[_, _, _]) = forT(tFor(p))
	/**
	  * @param p A point
	  * @return A point on this path that has the same t-axis position.
	  *         None if no matching point exists within this path's t-axis range.
	  */
	def liftMatching(p: NumericVectorLike[_, _, _]) = liftDtFor(p).map(forDt)
	/**
	  * @param p A point
	  * @return A point on this path most closely matching the specified point (in terms of its t-axis projection).
	  *         Limits to points on this path, meaning that the t-axis projections might not actually match.
	  */
	def capMatching(p: NumericVectorLike[_, _, _]) = capForT(tFor(p))
}
