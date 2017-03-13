package me.babaili.ajabberd
package protocol
package extensions
package register

import scala.xml._

private[ajabberd] object Builder extends ExtensionBuilder[Query]
{
    val tag = Query.tag
    val namespace = "jabber:iq:register"

    def apply(xml:Node):Query = RegistrationRequest(xml)
}




