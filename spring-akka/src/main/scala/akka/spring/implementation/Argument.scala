package akka.spring.implementation

;

import java.lang.annotation.Annotation

/**
 * stores information about a single argument to a handler method
 */
// class Argument(@BeanProperty var argumentType: Class[_], @BeanProperty var argumentPosition: Int, @BeanProperty var argumentAnnotation: Annotation)


class Argument(at: Class[_], ap: Int, aa: Annotation) {

  var _argumentType: Class[_] = at
  var _argumentPosition: Int = ap
  var _argumentAnnotation: Annotation = aa

  def argumentType = this._argumentType

  def argumentPosition = this._argumentPosition

  def argumentAnnotation = this._argumentAnnotation

  def argumentType_(at: Class[_]) {
    this._argumentType = at
  }

  def argumentPosition_(ap: Int) {
    this._argumentPosition = ap
  }

  def argumentAnnotation_(a: Annotation) {
    this._argumentAnnotation = a
  }


}