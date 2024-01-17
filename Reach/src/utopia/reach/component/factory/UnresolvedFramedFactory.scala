package utopia.reach.component.factory

import utopia.firmament.context.BaseContext
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible, StackLength}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.insets.SidedBuilder.SidesBuilder
import utopia.paradigm.shape.shape2d.insets.{HasSides, Sides}
import utopia.reach.component.factory.UnresolvedFramedFactory.{UnresolvedStackInsets, UnresolvedStackLength}
import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object UnresolvedFramedFactory
{
	// TYPES    --------------------------
	
	/**
	  * Type that represents either a resolved, or still unresolved stack length
	  */
	type UnresolvedStackLength = Either[SizeCategory, StackLength]
	/**
	  * Type that represents a set of insets that may still be fully or partially unresolved
	  */
	type UnresolvedStackInsets = Sides[UnresolvedStackLength]
	
	
	// ATTRIBUTES   ----------------------
	
	/**
	  * A factory used for building unresolved stack insets
	  */
	val sides = Sides[UnresolvedStackLength](Right(StackLength.fixedZero))
	
	
	// IMPLICIT --------------------------
	
	implicit def lengthToUnresolved(l: StackLength): UnresolvedStackLength = Right(l)
	implicit def unresolvedSize(s: SizeCategory): UnresolvedStackLength = Left(s)
	implicit def insetsToUnresolved(i: StackInsets): UnresolvedStackInsets =
		sides.withSides(i.sides.view.mapValues { Right(_) }.toMap)
	implicit def unresolvedSizes(s: HasSides[SizeCategory]): UnresolvedStackInsets =
		sides.withSides(s.sides.view.mapValues { Left(_) }.toMap)
}

/**
  * Common trait for component factories that apply framing.
  * However, the exact size of the frame may be unknown at this stage.
  * @author Mikko Hilpinen
  * @since 16/01/2024, v1.2
  */
trait UnresolvedFramedFactory[+Repr]
{
	// ABSTRACT ------------------------
	
	def insets: UnresolvedStackInsets
	
	/**
	  * @param insets Insets to apply, where each side is defined as either Left: An approximate size category, or
	  *               Right: A specific stack length
	  * @return Copy of this factory that uses the specified insets
	  */
	def withInsets(insets: UnresolvedStackInsets): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Copy of this factory, which doesn't place/allow any insets around the created component
	  */
	def withoutInsets = withInsets(UnresolvedFramedFactory.sides.empty)
	
	/**
	  * Resolves the specified insets in a specific context
	  * @param context Context in which the insets are resolved (implicit)
	  * @return Resolved insets
	  */
	def resolveInsetsIn(implicit context: BaseContext) =
		StackInsets.withSides(insets.sides.view.mapValues { _.rightOrMap { context.margins.around(_) } }.toMap)
	
	
	// OTHER    -----------------------
	
	/**
	  * Resolves the specified insets in a variable context environment
	  * @param contextPointer A (variable) context pointer
	  * @param hierarchy Applicable component hierarchy (call-by-name).
	  *                  Used for determining the context mapping conditions.
	  * @return A pointer that resolves into the applied insets, based on the applicable context
	  */
	def resolveVariableInsets(contextPointer: Changing[BaseContext], hierarchy: => ComponentHierarchy) = {
		val i = insets
		if (i.lengthsIterator.forall { _.isRight })
			Fixed(StackInsets.withSides(i.sides.view.mapValues { _.toOption.get }.toMap))
		else
			contextPointer.fixedValue match {
				case Some(c) => Fixed(resolveInsetsIn(c))
				// TODO: Once we have a better variable context class, map the margins instead of the whole context
				case None => contextPointer.mapWhile(hierarchy.linkPointer) { resolveInsetsIn(_) }
			}
	}
	
	/**
	  * @param insets Insets to place around the created component(s)
	  * @return Copy of this factory that places the specified insets
	  */
	def withInsets(insets: StackInsetsConvertible): Repr =
		withInsets(insets.toInsets.mapTo[UnresolvedStackLength] { Right(_) })
	/**
	  * @param insetSize The size of insets to place around the created component(s) on every side.
	  *                  None if no insets should be placed.
	  * @return Copy of this factory that places the specified insets
	  */
	def withInsets(insetSize: Option[SizeCategory]): Repr = insetSize match {
		case Some(size) => withInsets(size)
		case None => withoutInsets
	}
	/**
	  * @param insetSize The size of insets to place around the created component(s) on every side.
	  * @return Copy of this factory that places the specified insets
	  */
	def withInsets(insetSize: SizeCategory): Repr = withInsets(UnresolvedFramedFactory.sides.symmetric(Left(insetSize)))
	/**
	  * @param insets Insets (as sizes) to place around the created components.
	  *               Unspecified values will be interpreted as fixed zero-lengths.
	  * @return Copy of this factory that places the specified-sized insets
	  */
	def withInsets(insets: HasSides[SizeCategory]): Repr =
		withInsets(UnresolvedFramedFactory.sides.withSides(insets.sides.view.mapValues { Left(_) }.toMap))
	
	/**
	  * @param side Side to modify
	  * @param inset Inset to place to that side
	  * @return Copy of this factory with the specified inset placed
	  */
	def withInset(side: Direction2D, inset: UnresolvedStackLength) = mapInsets { _.withSide(side, inset) }
	/**
	  * @param axis Targeted axis
	  * @param inset Inset to place on both sides along that axis
	  * @return Copy of this factory with modified insets
	  */
	def withInsetsAlong(axis: Axis2D, inset: UnresolvedStackLength) = mapInsets { _.withAxis(axis, inset) }
	
	/**
	  * @param f A function that modifies the insets around the created components
	  * @return Copy of this factory that uses the modified insets
	  */
	def mapInsets(f: Mutate[UnresolvedStackInsets]) = withInsets(f(insets))
	
	/**
	  * Updates insets based on a building function
	  * @param f A function that accepts a builder.
	  *          The insets are updated based on the function's modifications to the builder.
	  * @tparam U Arbitrary function result type
	  * @return Copy of this factory with insets (partially) overwritten by the specified builder function
	  */
	def updateInsets[U](f: SidesBuilder[UnresolvedStackLength] => U) = {
		val builder = UnresolvedFramedFactory.sides.newBuilder
		f(builder)
		withInsets(insets ++ builder.result())
	}
	/**
	  * Assigns new insets based on a building function
	  * @param f A function that accepts a builder.
	  *          The insets are formed based on the function's modifications to the builder.
	  * @tparam U Arbitrary function result type
	  * @return Copy of this factory with insets generated by the specified builder function
	  */
	def buildInsets[U](f: SidesBuilder[UnresolvedStackLength] => U) = {
		val builder = UnresolvedFramedFactory.sides.newBuilder
		f(builder)
		withInsets(builder.result())
	}
}
