package me.babaili.ajabberd.util

import scala.xml.{Elem, Node}

/**
  * Created by cyq on 10/04/2017.
  */
object XMLUtil {

    def addChild(parent:Elem, child:Elem) = {
        parent match {
            case xml.Elem(prefix, label, attributes, scope, children @ _*) => {
                println("====== matched")
                xml.Elem(prefix, label, attributes, scope, children.isEmpty, children ++ child: _*)
            }
            case other => {
                println("====== other")
                other
            }
        }
    }

}
