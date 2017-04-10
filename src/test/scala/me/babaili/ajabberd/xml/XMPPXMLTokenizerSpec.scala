package me.babaili.ajabberd.xml

import me.babaili.ajabberd.protocol.{Features, SaslAuth, StartTls}
import org.scalatest.{FunSpecLike, Matchers}

/**
  * Created by cyq on 15/03/2017.
  */
class XMPPXMLTokenizerSpec  extends FunSpecLike with Matchers {

    describe("given an XMPPXMLTokenizer") {
        /*
        it("should be able to emit xmpp packets") {
             val startStream = "<?xml version=\"1.0\"?> \n" +
                "<stream:stream to=\"onepiece.com\" xmlns=\"jabber:client\" \n " +
                "xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"

            val startTls = "<stream:features> " +
                " <starttls xmlns=\"urn:ietf:params:xml:ns: xmpp-tls\"></starttls> " +
                " </stream:features>"

            val xmlTokenizer = new XmlTokenizer()
            val result = xmlTokenizer.decode((startStream + startTls).getBytes())
            result match {
                case Left(xmlEvents) =>
                    val (x,y) = XMPPXMLTokenizer.emit(xmlEvents)
                    println(x, y)
                    y.isEmpty match {
                        case false =>
                            val (x2, y2) = XMPPXMLTokenizer.emit(y)
                            println(x2,y2)
                            if (x2.head.isInstanceOf[Features]) {
                                val features = x2.head.asInstanceOf[Features]
                                features.features.foreach(println _)
                            } else {
                                println(s"what? ${x2.getClass()}")
                                assert(false)
                            }
                        case true =>
                            println("end")
                    }
                case Right(e) =>
                    println(e)
                    assert(false)
            }
        }


        it("should be able to emit xmpp packets for <auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">AGFhAGJiYg==</auth> ") {
            val auth = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">AGFhAGJiYg==</auth>"
            val xmlTokenizer = new XmlTokenizer()
            val result = xmlTokenizer.decode(auth.getBytes())
            result match {
                case Left(xMLEvents) =>
                    println(s"========== result length ${xMLEvents.length}")

                    val packets = XMPPXMLTokenizer.emit(xMLEvents)

                    println(s"=========== packets length ${packets._1.length} ${packets._2.length}")

                    val head = packets._1.head

                    println(s"============ head packet ${head.toString} ${head.getClass()}")

                    val auth = head.asInstanceOf[SaslAuth]
                    println(s"============= auth ${auth.mechanism}")


                case Right(e) =>
                    println(e)
                    assert(false)
            }
        }
        */

        it("should be able to emit xmpp packets for <starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>") {
            val xml = "<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>"
            val xmlTokenizer = new XmlTokenizer()
            val result = xmlTokenizer.decode(xml.getBytes())
            result match {
                case Left(xMLEvents) =>
                    println(s"========== result length ${xMLEvents.length}")

                    val packets = XMPPXMLTokenizer.emit(xMLEvents)

                    println(s"=========== packets length ${packets._1} ${packets._2.length}")

                    val head = packets._1.head

                    println(s"============ head packet ${head.toString} ${head.getClass()}")

                    val startTls = head.asInstanceOf[StartTls]
                    println(s"============= starttls ${startTls.toString}")


                case Right(e) =>
                    println(e)
                    assert(false)
            }
        }


        /*
        it("should be able to convert xml event to full xml") {
            val xmlString = "<stream:stream xmlns='jabber:client' a='b' to='localhost' " +
                "xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='aa@localhost' xml:lang='en'>"

            val xmlTokenizer = new XmlTokenizer()

            val result = xmlTokenizer.decode(xmlString.getBytes())

            result match {
                case Left(xmlEvents) =>
                    val xmlStringAgain = XMPPXMLTokenizer.xmlEventsToXml(xmlEvents)
                    println(xmlStringAgain)
                case Right(exception) =>
                    exception.printStackTrace()

            }
        }
        */
    }

}
