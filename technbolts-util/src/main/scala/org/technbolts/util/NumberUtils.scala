package org.technbolts.util

object NumberUtils {
  def clamp(min:Int, max:Int)(value:Int) =      if(value<min) min
                                           else if(value>max) max
                                           else value
}