package me.babaili.ajabberd
package protocol
package extensions
package muc
package admin

import scala.collection._
import scala.xml._

import Protocol._

private[ajabberd] object Builder extends ExtensionBuilder[X]
{
    val tag = X.tag
    val namespace = me.babaili.ajabberd.protocol.extensions.muc.general.Builder.namespace + "#admin"

    // FIXME: try to find a nicer way to do this, MUC standard is quite dirty
    def apply(xml:Node):X =
    {
        if (1 == ((xml \ "item") \ "@role").length)
        {
            return ChangeRole(xml)
        }
        else if (1 == ((xml \ "item") \ "@affiliation").length)
        {
            return ChangeAffiliation(xml)
        }
        else
        {
            return X(xml)
        }
    }

}
