package me.babaili.ajabberd.net

/**
  * Created by chengyongqiao on 11/02/2017.
  */

class XmlStreamException(val message: String) extends Exception {
    override def getMessage: String = message
}

object XmlStreamException {
    def apply(message: String) = new XmlStreamException(message)
}
