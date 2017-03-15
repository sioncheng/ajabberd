package me.babaili.ajabberd
package protocol

import scala.collection._
import scala.xml._

object Features
{
    val streamTag = "stream:features"
    val tag = "features"

    def apply(features:Seq[Node]):Features =
    {
        apply(Elem(null, streamTag, Null, TopScope, false, features:_*))
    }

    def apply(xml:Node):Features = new Features(xml)
}

class Features(xml:Node) extends XmlWrapper(xml) with Packet
{
    val features:Seq[Node] =  this.xml.child
}