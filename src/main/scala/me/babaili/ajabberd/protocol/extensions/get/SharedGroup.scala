package me.babaili.ajabberd
package protocol
package extensions
package get

import scala.xml.Node

/**
  * Created by chengyongqiao on 08/04/2017.
  */
class SharedGroup(xml: Node) extends Query(xml) {



}


object SharedGroup {
    val tag = "sharedgroup"
    val namespace = "http://www.jivesoftware.org/protocol/sharedgroup"
}