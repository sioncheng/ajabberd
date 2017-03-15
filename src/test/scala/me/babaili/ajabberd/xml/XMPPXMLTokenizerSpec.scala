package me.babaili.ajabberd.xml

import org.scalatest.{FunSpecLike, Matchers}

/**
  * Created by cyq on 15/03/2017.
  */
class XMPPXMLTokenizerSpec  extends FunSpecLike with Matchers {

    describe("") {
        it("") {
            val startTls = "<features> " +
                " <starttls xmlns=\"urn:ietf:params:xml:ns: xmpp-tls\"></starttls> " +
                " </features>"

            val xmlTokenizer = new XmlTokenizer()
            val result = xmlTokenizer.decode(startTls.getBytes())
            result match {
                case Left(xmlEvents) =>
                    val (x,y) = XMPPXMLTokenizer.emit(xmlEvents)
                    println(x, y)
                case Right(e) =>
                    println(e)
            }
        }
    }

}
