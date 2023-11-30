package org.clulab

package object skema {
  implicit class EnhancedCollections[A, C](coll: C)(implicit converter: C => Seq[A]) {
    def first(f: A => Boolean): Option[A] = {
      for (a <- coll) {
        if(f(a)) {
          return Some(a)
        }
      }
      None
    }
  }

  implicit class EnhancedType[A](a: A) {
    def let[B](f: A => B): B = f(a)
  }

  implicit class EnhancedResource[A <: {def close(): Unit}](r: A) {
    def use[B](f: (A) => B): B = {
      try {
        f(r)
      }
      finally {
        r.close()
      }
    }
  }

  def using[A <: {def close(): Unit}, B](closeable: A)(f: A => B): B = closeable.use(f)

  def first[A](seq: Seq[A])(f: A => Boolean): Option[A] = {
    for (a <- seq) {
      if(f(a)) {
        return Some(a)
      }
    }
    None
  }

  /**
   *
   * @param coll                         - the collection on which to call the methods
   * @param collectionToSequenceImplicit - just like a view bound: CT <% Seq[T], but written explicitly (view bounds
   *                                     are deprecated); Allows this code to work with any collections that can be
   *                                     viewed as Seq[T] via an implicit conversion
   * @param numeric                      - implicit param to be able to use the same methods for all number types
   */
  implicit class EnhancedNumericCollection[T, CT](coll: CT)(implicit collectionToSequenceImplicit: CT => Seq[T], numeric: Numeric[T]) {
    def mean(): Double = {
      // coll.sum might overflow
      var sum = 0.0
      for (n <- coll) {
        sum += numeric.toDouble(n)
      }
      sum / (coll.size.toDouble)
    }

    def variance(): Double = {
      var sumOfSquares = 0.0
      var sum = 0.0
      for (number <- coll) {
        val numberDouble = numeric.toDouble(number)
        sumOfSquares += numberDouble * numberDouble
        sum += numberDouble
      }
      sumOfSquares / coll.size - math.pow((sum / coll.size), 2)
    }
  }

}
