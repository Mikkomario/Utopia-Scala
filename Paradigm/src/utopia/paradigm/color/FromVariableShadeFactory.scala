package utopia.paradigm.color

/**
  * Common trait for factories that construct items based on shade, supporting variable (i.e. changing) shades.
  * @author Mikko Hilpinen
  * @since 02.11.2024, v1.7.1
  */
trait FromVariableShadeFactory[+A] extends FromShadeFactory[A]
{
	
}
