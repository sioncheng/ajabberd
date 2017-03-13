package me.babaili.ajabberd
package protocol
package extensions
package session

import scala.xml._

private[ajabberd] object Builder extends ExtensionBuilder[Session]
{
    val tag = Session.tag
    val namespace = "urn:ietf:params:xml:ns:xmpp-session"

    def apply(xml:Node):Session = Session(xml)
}
