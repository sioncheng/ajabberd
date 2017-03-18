package me.babaili.ajabberd.xmpp

import akka.actor.Actor
import akka.io.Tcp.Write
import akka.util.ByteString
import me.babaili.ajabberd.protocol._
import me.babaili.ajabberd.util

import scala.collection.immutable.HashMap


/**
  * Created by cyq on 14/03/2017.
  */

class UnexpectedPacketException(val message: String) extends Exception {
    override def getMessage: String = message
}

class XmppServer extends Actor with util.Logger {

    def receive = {
        case packets: List[Packet] =>
            debug(s"received packets ${packets.length}")
            handlePackets(packets)
        case _ =>
            error("what?")
    }

    def handlePackets(packets: List[Packet]):Unit = {
        packets.head match {
            case NullPacket =>
                warn("no packet has been emit")
            case _ =>
                var supported:Boolean = true
                if (packets.head.isInstanceOf[StreamHead]) {
                    val streamHead = packets.head.asInstanceOf[StreamHead]
                    val fromClient = streamHead.attributes.get("from").getOrElse("")
                    val responseAttributes = HashMap[String, String]("from" -> "localhost",
                        "to" -> fromClient)
                    val responseHead = StreamHead("jabber:client", responseAttributes)
                    debug(s"stream head ${streamHead}")
                    sender() ! XmlHead()
                    sender() ! responseHead


                    val tls = <starttls xmlns={Tls.namespace}>
                        <required/>
                    </starttls>

                    val mechanism = <mechanisms xmlns={Sasl.namespace}>
                        <mechanism>
                            {SaslMechanism.Plain.toString}
                        </mechanism>
                        <mechanism>
                            {SaslMechanism.DiagestMD5.toString}
                        </mechanism>
                    </mechanisms>

                    val features = Features(List(tls, mechanism))

                    sender() ! features

                    debug("accept start stream and response features")
                } else if (packets.head.isInstanceOf[StartTls]) {
                    val proceed = TlsProceed()
                    sender() ! proceed
                } else {
                    supported = false
                    warn(s"unsupported packet now ${packets.head}")
                    sender() ! new UnexpectedPacketException("packet is not stream head")
                }

                if(supported && packets.tail != null && !packets.tail.isEmpty) {
                    handlePackets(packets.tail)
                }

        }
    }
}
