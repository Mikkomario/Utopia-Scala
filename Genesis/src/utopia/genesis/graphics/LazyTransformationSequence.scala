package utopia.genesis.graphics

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}
import utopia.paradigm.shape.shape3d.Matrix3D

object LazyTransformationSequence
{
	/**
	  * Identity transformation with no parent
	  */
	val origin = root(Matrix3D.identity)
	
	/**
	  * @param transformation A transformation
	  * @return That transformation wrapped as a lazy transformation sequence
	  */
	def root(transformation: Matrix3D) = new LazyTransformationSequence(None, transformation)
}

/**
  * Used for lazily calculating a sequence of transformations, utilizing precalculated states when possible
  * @author Mikko Hilpinen
  * @since 28.1.2022, v2.6.3
  * @param parent The sequence over this part
  * @param transformation A transformation to apply on top of the parent sequence
  */
class LazyTransformationSequence(val parent: Option[LazyTransformationSequence], transformation: Matrix3D)
	extends Lazy[Matrix3D]
		with LinearTransformable[LazyTransformationSequence] with AffineTransformable[LazyTransformationSequence]
{
	// ATTRIBUTES   --------------------------
	
	private val cache = Lazy {
		parent match {
			case Some(parent) => parent.value(transformation)
			case None => transformation
		}
	}
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return An iterator that iterates over parent transformation states, from bottom to top
	  */
	def parentStatesIterator = OptionsIterator.iterate(parent) { _.parent }
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override def current = cache.current
	override def value: Matrix3D = cache.value
	
	override def transformedWith(transformation: Matrix2D): LazyTransformationSequence =
		transformedWith(transformation.to3D)
	override def transformedWith(transformation: Matrix3D) = new LazyTransformationSequence(Some(this), transformation)
}
