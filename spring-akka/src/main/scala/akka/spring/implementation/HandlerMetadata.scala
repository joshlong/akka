package akka.spring.implementation

import java.lang.reflect.Method


class HandlerMetadata {

  var _method: Method = _
  var _selfReference: Option[Argument] = None
  var _actorContextReference: Option[Argument] = None
  var _payload: Option[Argument] =  None

  def method = this._method

  def selfReference = this._selfReference

  def actorContextReference = this._actorContextReference

  def payload = this._payload

  def method_=(m: Method) {
    this._method = m
  }

  def selfReference_=(arg: Option[Argument]) {
    this._selfReference = arg
  }


  def actorContextReference_=(arg: Option[Argument]) {
    this._actorContextReference = arg
  }

  def payload_=(arg: Option[Argument]) {
    this._payload = arg
  }

}