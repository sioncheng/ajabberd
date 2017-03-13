package me.babaili.ajabberd
package protocol
package extensions
package forms

import scala.collection._
import scala.xml._

import fields._

import Protocol._

object ResultHeader
{
    val tag = "reported"

    def apply(fields:Seq[Field]):ResultHeader = apply(build(fields))

    def apply(xml:Node):ResultHeader = new ResultHeader(xml)

    def build(fields:Seq[Field]):Node =
    {
    	val ffs = Protocol.seqwrapper2seqnode(fields)
        Elem(null, tag, Null, TopScope, ffs: _*)
    }
}

class ResultHeader(xml:Node) extends XmlWrapper(xml)
{
}