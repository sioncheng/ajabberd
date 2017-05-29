package me.babaili.ajabberd.net

import javax.xml.stream.events.XMLEvent

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.util.ByteString
import me.babaili.ajabberd.net.TcpConnectionHandler.CloseAnyway
import me.babaili.ajabberd.protocol.{JID, Packet, SaslSuccess, TlsProceed}
import me.babaili.ajabberd.util.Logger
import me.babaili.ajabberd.xml.{XMPPXMLTokenizer, XmlTokenizer}
import me.babaili.ajabberd.xmpp.{XmppEvents, XmppPackets, XmppStreamConnection}

/**
  * Created by sion on 2/4/17.
  */

object TcpConnectionHandler {

    object Status {
        val UNKNOWN = 0
        val INIT = 1
        val TLS = 2
        val CLOSED = 100
    }

    case class CloseAnyway()
}

class TcpConnectionHandler(tcpListener: ActorRef, name: String) extends Actor {

    import TcpConnectionHandler.Status

    val logger = new Logger {}
    var xmlTokenizer = new XmlTokenizer

    var tcpConnection: ActorRef = null
    var status = Status.INIT
    var remainedXmlEvents: List[XMLEvent] = List.empty
    var xmppConnection: ActorRef = null
    var sslEngine: ActorRef = null
    var uid: String = ""
    var jid: JID = null



    def receive: Receive = {
        case Received(data) =>
            if (tcpConnection == null) {
                tcpConnection = sender()
            }
            logger.debug(s"${name} at status ${status} received data ${data.decodeString("utf8")}")
            status match {
                case Status.INIT =>
                    val result = xmlTokenizer.decode(data.toArray)
                    result match {
                        case Left(xmlEvents) =>
                            var continue: Boolean = true
                            remainedXmlEvents = remainedXmlEvents ::: xmlEvents
                            while(continue) {
                                val (packets, remained) = XMPPXMLTokenizer.emit(remainedXmlEvents)
                                if (packets.isEmpty == false) {
                                    remainedXmlEvents = remained
                                    if (xmppConnection == null) {
                                        xmppConnection = context.actorOf(Props(classOf[XmppStreamConnection]))
                                    }
                                    xmppConnection ! XmppPackets(packets.get)
                                    if (remainedXmlEvents.length > 0) {
                                        continue = true
                                    } else {
                                        continue = false
                                    }
                                } else {
                                    logger.warn("no packets have been emitted")
                                    continue = false
                                }
                            }
                        case Right(exception) =>
                            //
                            processXmlStreamException(exception)
                    }
                case Status.TLS =>
                    sslEngine ! SslEngine.SslData(data.toArray)
                case x =>
                    logger.error(s"what status ? ${x}")
            }
        case x: Packet =>
            status match {
                case Status.INIT =>
                    tcpConnection ! Write(ByteString.fromString(x.toString()))
                    if (x.isInstanceOf[TlsProceed]) {
                        status = Status.TLS
                        sslEngine = context.actorOf(Props(classOf[SslEngine]))
                        xmlTokenizer = new XmlTokenizer
                    }
                case Status.TLS =>
                    logger.debug(s"send wrap request ${x.toString()}")
                    sslEngine ! SslEngine.WrapRequest(x.toString().getBytes())

                    if (x.isInstanceOf[SaslSuccess]) {
                        xmlTokenizer = new XmlTokenizer
                    }
                case _ =>
                    logger.warn(s"what status ${status} for send packet ${x}")
            }
        case SslEngine.WrappedData(bs) =>
            tcpConnection ! Write(ByteString.fromArray(bs))
            logger.debug("send tls wrapped data to client")
        case SslEngine.FinishedHandshake() =>
            logger.debug("ssl engine finished handshake")
        case SslEngine.UnwrappedData(data) =>
            logger.debug(s"unwrapped data ${new String(data)}")
            val result = xmlTokenizer.decode(data)
            result match {
                case Left(xmlEvents) =>
                    var continue: Boolean = true
                    remainedXmlEvents = remainedXmlEvents ::: xmlEvents
                    while(continue) {
                        val (packets, remained) = XMPPXMLTokenizer.emit(remainedXmlEvents)
                        if (packets.isEmpty == false) {
                            remainedXmlEvents = remained
                            if (xmppConnection == null) {
                                xmppConnection = context.actorOf(Props(classOf[XmppStreamConnection]))
                            }
                            xmppConnection ! XmppPackets(packets.get)
                            if (remainedXmlEvents.length > 0) {
                                continue = true
                            } else {
                                continue = false
                            }
                        } else {
                            logger.warn("no packets have been emitted")
                            continue = false
                        }
                    }
                case Right(exception) =>
                    //
                    processXmlStreamException(exception)
            }
        case XmppEvents.JidAssign(jidValue, uidValue) =>
            jid = jidValue
            uid = uidValue

        //net close
        case PeerClosed =>
            //
            logger.debug("peer closed")
            tcpConnection ! akka.io.Tcp.Close
            import context.dispatcher
            context.system.scheduler.scheduleOnce(1 seconds, self, CloseAnyway())
        //processClose()
        case Closed =>
            logger.debug("closed")
            processClosed()
        case CloseAnyway() =>
            logger.debug("close anyway")
            processClosed()
    }



    def processXmlStreamException(e: Exception): Unit = {
        //
        logger.error("tcp connection handler received wrong xml data: " + e.getMessage(), e)
        sender() ! Close //will receive a closed message
    }

    def processClosed(): Unit = {
        status = Status.CLOSED
        tcpListener ! TcpListener.CloseTcpConnectionHandler(name)
        context.stop(self)
        if (sslEngine != null) {
            context.stop(sslEngine)
        }

        if (xmppConnection != null) {
            xmppConnection ! XmppEvents.ConnectionClosed(jid, uid)
        }
    }


}
