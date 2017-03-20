package me.babaili.ajabberd
package protocol

import scala.collection._
import scala.xml._

case class XmlHead() extends Packet {
    override def toString: String = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
}

object StreamHead
{
    protected [protocol] val tag = "<stream:stream"

    def qualifies(string:String):Boolean = string.indexOf(tag) >= 0

    def apply(string:String):StreamHead =
    {

        println("++++++++++++++ stream head")
        println(string)
        println("++++++++++++++ stream head")

        val xml = XML.loadString(string + StreamTail.tag)
        val attributes = xml.attributes.map( attribute => attribute.key -> attribute.value(0).text )
        apply(xml.scope.uri, attributes.toMap)
    }

    def apply(namespace:String, attributes:Map[String, String]):StreamHead = new StreamHead(namespace, attributes)
}

class StreamHead(val namespace:String, val attributes:Map[String, String]) extends Packet
{
    def findAttribute(name:String):Option[String] = attributes.get(name)

    override def toString:String = StreamHead.tag + " xmlns:stream='http://etherx.jabber.org/streams' xmlns='" + namespace + "'" + attributes.map( attribute => attribute._1 + "='" + attribute._2  + "'" ).mkString(" ", " ", ">")
}

object StreamTail
{
    protected[protocol] val tag = "</stream:stream>"

    def qualifies(string:String):Boolean = string == tag

    def apply():StreamTail = new StreamTail()
}

class StreamTail extends Packet
{
    override def toString:String = StreamTail.tag
}
