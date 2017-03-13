package me.babaili.ajabberd
package protocol
package extensions
package forms

import scala.collection._
import scala.xml._

import fields._

import Protocol._

object ResultItem
{
    val tag = "item"

    def apply(fields:Seq[Field]):ResultItem = apply(build(fields))

    def apply(xml:Node):ResultItem = new ResultItem(xml)

    def build(fields:Seq[Field]):Node =
    {
    	val ffs = Protocol.seqwrapper2seqnode(fields)
        Elem(null, tag, Null, TopScope, ffs:_*)
    }
}

class ResultItem(xml:Node) extends XmlWrapper(xml)
{
}