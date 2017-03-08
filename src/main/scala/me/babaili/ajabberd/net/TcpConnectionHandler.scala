package me.babaili.ajabberd.net

import javax.xml.stream.events.XMLEvent

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import me.babaili.ajabberd.xml.XmlTokenizer
import com.typesafe.scalalogging.Logger

/**
  * Created by chengyongqiao on 03/02/2017.
  */

object TcpConnectionHandler {
    val EXPECT_START_STREAM = 0
    val EXPECT_START_TLS = 1
    val EXPECT_PROCESS_TLS = 2

    val logger = Logger("me.babaili.ajabberd.net.TcpConnectionHandler")
}

class TcpConnectionHandler(tcpListener: ActorRef, name: String) extends Actor {
    import Tcp._
    import TcpConnectionHandler._

    var status = EXPECT_START_STREAM

    val logTitle = "tcp connection handler " + name + " "

    val xmlTokenizer = new XmlTokenizer()

    var tcpConnection: ActorRef = null
    var sslEngine: ActorRef = null

    def receive = {
        case d @ Received(data) =>
            //
            tcpConnection = sender()
            logger.debug(s"status ${status}")
            status match {
                case EXPECT_PROCESS_TLS =>
                    sslEngine ! d
                    logger.debug(s"forward receive data to ssl engine")
                case _ =>
                    val result = xmlTokenizer.decode(data.toArray)
                    result match {
                        case Left(xmlEvents) =>
                            //
                            status match {
                                case EXPECT_START_STREAM => expectStartStream(xmlEvents)
                                case EXPECT_START_TLS => expectStartTls(xmlEvents)
                                case x : Any => logger.debug("what status ?" + x.toString())
                            }
                        case Right(exception) =>
                            //
                            processXmlStreamException(exception)
                    }
            }

        case PeerClosed =>
            //
            logger.debug(logTitle + "peer closed")
            processClose()
        case Closed =>
            logger.debug(logTitle + "closed")
            processClose()
        case SslEngine.WrappedData(bs) =>
            tcpConnection ! Write(bs)
        case SslEngine.UnwrappedData(bs) =>
            val decodedString = bs.decodeString("UTF-8")
            logger.debug(s"client data ${decodedString}")
        case x =>
            logger.debug(logTitle + "what? " + x.toString())
    }

    def expectStartStream(xmlEvents: List[XMLEvent]): Unit = {
        //

        if (!xmlEvents.head.isStartDocument()) {
            val errorMessage = "first xml event is not start document while expect start stream"
            logger.debug(logTitle, errorMessage)
            processXmlStreamException(XmlStreamException(errorMessage))
        } else {
            val tail = xmlEvents.tail
            if (!tail.head.isStartElement()){
                val errorMessage = "second xml event is not start element while expect start stream"
                logger.debug(logTitle, errorMessage)
                processXmlStreamException(XmlStreamException(errorMessage))
            } else {
                val localPort = tail.head.asStartElement().getName().getLocalPart()
                if (! "stream".equalsIgnoreCase(localPort)){
                    val errorMessage = "second xml event is not stream while expect start stream"
                    logger.debug(logTitle, errorMessage)
                    processXmlStreamException(XmlStreamException(errorMessage))
                } else {
                    status = EXPECT_START_TLS
                    val stream = "<?xml version=\"1.0\"?>" +
                        "<stream:stream from=\"bleach.com\" id=\"someid\" xmlns=\"jabber:client\" " +
                        " xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"
                    sender() ! Write(ByteString.fromString(stream))
                    val startTls = "<stream:features> " +
                        " <starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls> " +
                        " <mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
                        "    <mechanism>DIGEST-MD5</mechanism> " +
                        "    <mechanism>PLAIN</mechanism> " +
                        " </mechanisms> " +
                        " </stream:features>"
                    sender() ! Write(ByteString.fromString(startTls))
                    logger.debug(logTitle + "accept start stream and response features")
                }
            }
        }
    }

    def expectStartTls(xmlEvents: List[XMLEvent]): Unit = {
        val head = xmlEvents.head
        if (!head.isStartElement()) {
            val errorMessage = "first xml event is not start document while expect start tls"
            logger.debug(errorMessage)
            processXmlStreamException(XmlStreamException(errorMessage))
        } else {
            val name = head.asStartElement().getName().getLocalPart()
            if (! "starttls".equalsIgnoreCase(name)) {
                val errorMessage = "name of first xml event is not start tls document while expect start tls"
                logger.debug(errorMessage)
                processXmlStreamException(XmlStreamException(errorMessage))
            } else {
                val proceed = "<proceed xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>"
                logger.debug(proceed)
                sender() ! Write(ByteString.fromString(proceed))
                status = EXPECT_PROCESS_TLS
                sslEngine = context.actorOf(Props(classOf[SslEngine]))
            }
        }

    }

    def processXmlStreamException(e: Exception): Unit = {
        //
        logger.error("tcp connection handler received wrong xml data: " + e.getMessage(), e)
        sender() ! Close //will receive a closed message
    }

    def processClose(): Unit = {
        tcpListener ! TcpListener.CloseTcpConnectionHandler(name)
        context.stop(self)
        if (sslEngine != null) {
            context.stop(sslEngine)
        }
    }
}
