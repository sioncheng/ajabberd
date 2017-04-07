package me.babaili.ajabberd.xmpp

import akka.actor.{Actor, ActorRef}
import me.babaili.ajabberd.auth.MySaslServer
import me.babaili.ajabberd.data.Passport
import me.babaili.ajabberd.protocol.{message, _}
import me.babaili.ajabberd.util
import me.babaili.ajabberd.global.ApplicationContext
import me.babaili.ajabberd.protocol.extensions.Query
import me.babaili.ajabberd.protocol.message.Message
import me.babaili.ajabberd.util.Logger
import me.babaili.ajabberd.xml.XMPPXMLTokenizer
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.collection.immutable.HashMap


/**
  * Created by cyq on 14/03/2017.
  */

class UnexpectedPacketException(val message: String) extends Exception {
    override def getMessage: String = message
}

object XmppStreamConnection {
    val INIT = 0
    val EXPECT_SASL = 1
    val EXPECT_NEW_STREAM2 = 2

    object Status {
        val UNKNOWN = 0
        val INIT = 1
        val AUTHENTICATION_NEGOTIATION = 2
        val EXPECT_SASL = 3
        val AUTHENTICATED = 4
        val EXPECT_NEW_SESSION = 5
        val PREPARE_TALK = 6
        val CLOSED = 100
    }
}

class XmppStreamConnection extends Actor {

    import XmppStreamConnection.Status

    val logger = new Logger {}

    var status = Status.INIT
    var saslServer = new MySaslServer()
    var clientConnection: ActorRef = null
    var jid: JID = new JID("aa","localhost","")
    var uid: String = ""

    def receive: Receive = {
        case packets: List[Packet] =>
            logger.debug(s"received packets ${packets.length}")
            handlePackets(packets)
        case XmppEvents.Response(action, data) =>
            action match {
                case XmppEvents.IqResult =>
                    clientConnection ! data
                case XmppEvents.MessageResponse =>
                    clientConnection ! data
                case x =>
                    logger.warn(s"what response ? ${x}")
            }
        case ja @ XmppEvents.JidAssign(jidValue, uidValue) =>
            jid = jidValue
            uid = uidValue
            clientConnection ! ja
        case x =>
            logger.error(s"what message ????? ${x}")
    }

    def handlePackets(packets: List[Packet]): Unit = {
        packets.head match {
            case NullPacket =>
                logger.warn("no packet has been emit")
            case _ =>

                if (clientConnection == null) {
                    clientConnection = sender()
                }

                var supported: Boolean = true

                val headPacket = packets.head
                logger.debug(s"head xml ${headPacket.toString()}")

                status match {
                    case Status.INIT =>
                        if (headPacket.isInstanceOf[StreamHead]) {
                            val streamHead = packets.head.asInstanceOf[StreamHead]
                            val fromClient = streamHead.attributes.get("from").getOrElse("")
                            uid = fromClient.split("@")(0)

                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            logger.debug(s"stream head ${streamHead}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val tls = <starttls xmlns={Tls.namespace}>
                                <optional/>
                            </starttls>

                            val mechanism = <mechanisms xmlns={Sasl.namespace}>
                                <mechanism>
                                    {SaslMechanism.Plain.toString}
                                </mechanism>
                                <mechanism>
                                    {SaslMechanism.DiagestMD5.toString}
                                </mechanism>
                                <optional/>
                            </mechanisms>

                            val features = Features(List(tls, mechanism))

                            sender() ! features

                            status = Status.AUTHENTICATION_NEGOTIATION
                        } else {
                            supported = false
                            logger.error(s"wrong data ${headPacket.toString()} coming during status ${status}")
                        }
                    case Status.AUTHENTICATION_NEGOTIATION =>

                        if (headPacket.isInstanceOf[StartTls]) {
                            val proceed = TlsProceed()
                            sender() ! proceed
                            status = Status.EXPECT_SASL
                        } else if (packets.head.isInstanceOf[iq.IQ]) {
                            if (clientConnection == null) {
                                clientConnection = sender()
                            }
                            val iqStanza = packets.head.asInstanceOf[iq.IQ]


                            logger.debug(s"iq ${iqStanza.toString}")

                            iqStanza match {
                                case get@iq.Get(oid, oTo, oFrom, Some(extension)) =>
                                    val ns = extension.namespace.getOrElse("")
                                    ns match {
                                        case extensions.auth.AuthenticationRequest.namespace =>

                                            logger.debug(s"get ${get} ${oid}, ${oTo}, ${oFrom}")

                                            val ar = extension.asInstanceOf[extensions.auth.AuthenticationRequest]
                                            uid = ar.username

                                            val id = oid.getOrElse("")


                                            val xml = <iq type='result' id={id}>
                                                <query xmlns='jabber:iq:auth'>
                                                    <username>{ar.username}</username>
                                                    <password/>
                                                    <digest/>
                                                    <resource/>
                                                </query>
                                            </iq>

                                            val res = iq.Result(xml)
                                            logger.debug(s"response to iq get auth ${xml.toString()}")
                                            sender() ! res
                                    }


                            }

                        } else {
                            logger.warn(s"unknown packet duration status ${status}")
                        }
                    case Status.EXPECT_SASL =>
                        if (headPacket.isInstanceOf[StreamHead]) {
                            val streamHead = packets.head.asInstanceOf[StreamHead]
                            val fromClient = streamHead.attributes.get("from").getOrElse("")
                            uid = fromClient.split("@")(0)

                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            logger.debug(s"stream head ${streamHead} at ${status}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val mechanism = <mechanisms xmlns={Sasl.namespace}>
                                <mechanism>{SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                            </mechanisms>

                            val comprehension = <compression xmlns="http://jabber.org/features/compress">
                                <method>zlib</method>
                            </compression>

                            val features = Features(List(mechanism, comprehension))

                            sender() ! features
                        } else if (packets.head.isInstanceOf[SaslAuth]) {

                            val auth = packets.head.asInstanceOf[SaslAuth]
                            auth.mechanism match {
                                case SaslMechanism.Plain =>
                                    val decodedString = (new BASE64Decoder()).decodeBuffer(auth.value)
                                    logger.debug(s"plain auth ${decodedString}")
                                    sender() ! SaslSuccess(auth.value)
                                    status = Status.EXPECT_NEW_SESSION

                                case SaslMechanism.DiagestMD5 =>
                                    logger.debug("digest md5 auth")
                                    val (_, response) = saslServer.evaluateResponse(null)
                                    val challengeBase64 = (new BASE64Encoder()).encode(response).replace("\r","").replace("\n","")
                                    logger.debug(s"challengeBase64 ${challengeBase64}")

                                    val challenge = SaslChallenge(challengeBase64)

                                    sender() ! challenge
                                case _ =>
                                    logger. warn("unknown mechanism")

                            }

                        } else if (packets.head.isInstanceOf[SaslResponse]) {
                            val response = packets.head.asInstanceOf[SaslResponse]
                            logger.debug(s"response text ${response.responseText}")

                            val decodedResponseBytes = (new BASE64Decoder()).decodeBuffer(response.responseText)
                            val decodedResponseText = new String(decodedResponseBytes)
                            logger.debug(s"decoded responseText ${decodedResponseText}")
                            val (ok, challenge) = saslServer.evaluateResponse(decodedResponseBytes)
                            ok match {
                                case true =>
                                    logger.debug("sasl success")
                                    val challengeBase64 = (new BASE64Encoder()).encode(challenge)
                                    sender() ! SaslSuccess(challengeBase64)
                                    status = Status.EXPECT_NEW_SESSION
                                case false =>
                                    val challengeBase64 = (new BASE64Encoder()).encode(challenge).replace("\r", "").replace("\n", "")
                                    logger.debug(s"challengeBase64 again ${challengeBase64}")
                                    sender() ! SaslChallenge(challengeBase64)
                            }
                        } else  {
                            logger.warn(s"unknown packet duration status ${status}")
                        }

                    case Status.EXPECT_NEW_SESSION =>
                        if (headPacket.isInstanceOf[StreamHead]) {
                            val streamHead = packets.head.asInstanceOf[StreamHead]
                            val fromClient = streamHead.attributes.get("from").getOrElse("")
                            uid = fromClient.split("@")(0)

                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "id" -> fromClient,
                                "version" -> "1.0")
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            logger.debug(s"new session ${streamHead}")

                            sender() ! XmlHead()

                            sender() ! responseHead

                            val compression = <compression xmlns="http://jabber.org/features/compress">
                                <method>zlib</method>
                            </compression>
                            val bind = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/>
                            val session = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
                            val register = <register xmlns='http://jabber.org/features/iq-register'/>
                            val features = Features(List(compression, bind, session, register))
                            sender() ! features

                            status = Status.PREPARE_TALK
                        } else {
                            logger.warn(s"unknown packet duration status ${status}")
                        }

                    case Status.PREPARE_TALK =>
                        if (headPacket.isInstanceOf[iq.Set]) {
                            headPacket match {
                                case set@iq.Set(oId, oTo, oFrom, Some(extension)) =>
                                    println(s"%%%%%%%%%%%%  extension ${extension.toString()}")
                                    if (extension.isInstanceOf[extensions.bind.BindRequest]) {
                                        val bind = extension.asInstanceOf[extensions.bind.BindRequest]
                                        val idValue = oId.getOrElse("bind_2")
                                        val resource = bind.resource.getOrElse("")
                                        if (jid == JID.EmptyJID) {
                                            jid = JID(uid, "localhost", resource)
                                        }
                                        val xml = <iq type='result' id={idValue}>
                                            <bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>
                                                <jid>aa@localhost/
                                                    {resource}
                                                </jid>
                                            </bind>
                                        </iq>

                                        sender() ! iq.Result(xml)
                                    } else if (extension.isInstanceOf[extensions.session.Session]) {
                                        val idValue = oId.getOrElse("bind_2")
                                        val xml = <iq from='localhost' type='result' id={idValue}/>

                                        sender() ! iq.Result(xml)
                                    } else if (extension.isInstanceOf[extensions.Query]) {
                                        val query = extension.asInstanceOf[extensions.Query]
                                        val ns = query.namespace.getOrElse("")
                                        ns match {
                                            case extensions.privacy.PrivacyQuery.namespace =>
                                                println(s"~~~~~~~~~~~~~  ${ns}")
                                        }

                                        logger.debug(s"${query.toString()}")
                                    } else {
                                        logger.warn(s"cant deal with iq set duration status ${status}")
                                    }
                                case get@iq.Get(oId, oTo, oFrom, Some(extension)) =>
                                    if (extension.isInstanceOf[extensions.Query]) {
                                        val query = extension.asInstanceOf[extensions.Query]
                                        query.namespace.getOrElse("") match {
                                            case "http://jabber.org/protocol/disco#items" =>
                                                val idValue = oId.getOrElse("")
                                                val toJid = oFrom.getOrElse(JID.EmptyJID).toString
                                                val xml = <iq from='localhost' type='result' id={idValue} to={toJid}>
                                                    <query xmlns='http://jabber.org/protocol/disco#items'>
                                                        <item jid='aa@localhost'/>
                                                        <item jid='bb@localhost'/>
                                                        <item jid='cc@localhost'/>
                                                    </query>
                                                </iq>

                                                sender() ! iq.Result(xml)
                                            case x =>
                                                logger.warn(s"unknown query ${x} duration status ${status}")
                                        }
                                    } else {
                                        logger.warn(s"cant deal with iq get duration status ${status}")
                                    }
                                case set@iq.Set(oId, oTo, oFrom, oExtension) =>
                                    oExtension.isEmpty match {
                                        case false =>
                                            val query = oExtension.get.asInstanceOf[extensions.Query]
                                            val namespace = query.namespace.getOrElse("")
                                            namespace match {
                                                case "jabber:iq:privacy" =>
                                                    logger.debug("jabber:iq:privacy")
                                                    //todo
                                                case x =>
                                                    logger.debug(s"namespace ${x}")
                                            }
                                        case true =>
                                            logger.warn(s"empty query ${set.toString}")
                                    }
                                    logger.debug(s"set set ${set} ")
                                    logger.debug(s"extension ${oExtension}")
                                    val idValue = oId.getOrElse("bind_2")
                                    val xml = <iq from='localhost' type='result' id={idValue}/>

                                    sender() ! iq.Result(xml)
                                case x =>
                                    logger.warn(s"unknown iq set during status ${status}")
                                    logger.warn(s"${x}")
                            }
                        } else if (headPacket.isInstanceOf[iq.Get]) {
                            headPacket match {
                                case get@iq.Get(oId, oTo, oFrom, oExtension) =>
                                    oExtension.isEmpty match {
                                        case false =>
                                            val query = oExtension.get.asInstanceOf[extensions.Query]
                                            query.namespace.getOrElse("") match {
                                                case "http://jabber.org/protocol/disco#items" =>
                                                    val idValue = oId.getOrElse("")
                                                    val toJid = oFrom.getOrElse(JID.EmptyJID).toString

                                                    val xml = <iq from='localhost' type='result' id={idValue} to={toJid}>
                                                        <query xmlns='http://jabber.org/protocol/disco#items'>
                                                            <item jid='aa@localhost' name="aa" subscription="both"/>
                                                            <item jid='bb@localhost' name="bb" subscription="both"/>
                                                            <item jid='cc@localhost' name="cc" subscription="both"/>
                                                        </query>
                                                    </iq>

                                                    sender() ! iq.Result(xml)
                                                case "jabber:iq:roster" =>
                                                    val idValue = oId.getOrElse("")
                                                    val toJid = oFrom.getOrElse(JID.EmptyJID).toString

                                                    val xml = <iq id={idValue} type="result">
                                                        <query xmlns="jabber:iq:roster">
                                                            <item jid="aa@localhost" name="aa" subscription="both">
                                                                <group>Friends</group>
                                                            </item>
                                                            <item jid="bb@localhost" name="bb" subscription="both">
                                                                <group>Friends</group>
                                                            </item>
                                                            <item jid="cc@localhost" name="cc" subscription="both">
                                                                <group>Friends</group>
                                                            </item>
                                                        </query>
                                                    </iq>

                                                    logger.debug(s"your friends ${xml.toString()}")
                                                    sender() ! iq.Result(xml)
                                                case "http://jabber.org/protocol/disco#info" =>
                                                    //xep-0030
                                                    val idValue = oId.getOrElse("")
                                                    val toJid = oFrom.getOrElse(JID.EmptyJID).toString

                                                    val xml = <iq type='result' from='localhost' to={toJid} id={idValue}>
                                                        <query xmlns='http://jabber.org/protocol/disco#info'>
                                                            <identity
                                                            category='conference'
                                                            type='text'
                                                            name='Play-Specific Chatrooms'/>
                                                            <identity
                                                            category='directory'
                                                            type='chatroom'
                                                            name='Play-Specific Chatrooms'/>
                                                            <feature var='http://jabber.org/protocol/disco#info'/>
                                                            <feature var='http://jabber.org/protocol/disco#items'/>
                                                            <feature var='http://jabber.org/protocol/muc'/>
                                                            <feature var='jabber:iq:register'/>
                                                            <feature var='jabber:iq:search'/>
                                                            <feature var='jabber:iq:time'/>
                                                            <feature var='jabber:iq:version'/>
                                                        </query>
                                                    </iq>

                                                    logger.debug(s"my services ${xml.toString()}")
                                                    sender() ! iq.Result(xml)
                                                case "" =>
                                                    logger.warn("??? empty namespace")
                                                case x =>
                                                    logger.error(s"??? unknown namespace ${x} of iq get on status ${status}")

                                                    sender() ! iq.Result(oId,oTo,oFrom,None)
                                            }
                                        case true =>
                                            logger.warn("??? empty extension")

                                            val idValue = oId.getOrElse("v1")
                                            if (headPacket.toString().indexOf("vCard") > 0) {
                                                val xml = <iq id={idValue} to='bb@localhost' type='result'>
                                                    <vCard xmlns='vcard-temp'>
                                                        <FN>Peter Saint-Andre</FN>
                                                        <N>
                                                            <FAMILY>Saint-Andre</FAMILY>
                                                            <GIVEN>Peter</GIVEN>
                                                            <MIDDLE/>
                                                        </N>
                                                        <NICKNAME>stpeter</NICKNAME>
                                                        <URL>http://www.xmpp.org/xsf/people/stpeter.shtml</URL>
                                                        <BDAY>1966-08-06</BDAY>
                                                        <ORG>
                                                            <ORGNAME>XMPP Standards Foundation</ORGNAME>
                                                            <ORGUNIT/>
                                                        </ORG>
                                                        <TITLE>Executive Director</TITLE>
                                                        <ROLE>Patron Saint</ROLE>
                                                        <TEL><WORK/><VOICE/><NUMBER>303-308-3282</NUMBER></TEL>
                                                        <TEL><WORK/><FAX/><NUMBER/></TEL>
                                                        <TEL><WORK/><MSG/><NUMBER/></TEL>
                                                        <ADR>
                                                            <WORK/>
                                                            <EXTADD>Suite 600</EXTADD>
                                                            <STREET>1899 Wynkoop Street</STREET>
                                                            <LOCALITY>Denver</LOCALITY>
                                                            <REGION>CO</REGION>
                                                            <PCODE>80202</PCODE>
                                                            <CTRY>USA</CTRY>
                                                        </ADR>
                                                        <TEL><HOME/><VOICE/><NUMBER>303-555-1212</NUMBER></TEL>
                                                        <TEL><HOME/><FAX/><NUMBER/></TEL>
                                                        <TEL><HOME/><MSG/><NUMBER/></TEL>
                                                        <ADR>
                                                            <HOME/>
                                                            <EXTADD/>
                                                            <STREET/>
                                                            <LOCALITY>Denver</LOCALITY>
                                                            <REGION>CO</REGION>
                                                            <PCODE>80209</PCODE>
                                                            <CTRY>USA</CTRY>
                                                        </ADR>
                                                        <EMAIL><INTERNET/><PREF/><USERID>stpeter@jabber.org</USERID></EMAIL>
                                                        <JABBERID>stpeter@jabber.org</JABBERID>
                                                        <DESC>
                                                            More information about me is located on my
                                                            personal website: http://www.saint-andre.com/
                                                        </DESC>
                                                    </vCard>
                                                </iq>

                                                sender() ! iq.Result(xml)

                                            } else {
                                                sender() ! iq.Result(oId,oTo,oFrom,None)
                                            }


                                    }
                                case x =>
                                    logger.warn(s"unknown get ${x} duration status ${status}")
                            }
                        } else if (headPacket.isInstanceOf[presence.Presence]) {
                            val pre = headPacket.asInstanceOf[presence.Presence]
                            logger.debug(s"presence ${pre.toString}")

                            def tellFriendStatus(from: JID): Unit = {
                                val xml = <presence from={from.toString()} to={jid.toString()} type="subscribed"/>

                                sender() ! Stanza(xml)

                                val xml2 = <presence from={from.toString()} to={jid.toString()}>
                                    <show>chat</show>
                                    <status>hello</status>
                                    <priority>1</priority>
                                </presence>

                                sender() ! Stanza(xml2)
                            }

                            //tellFriendStatus(JID("aa@localhost"))
                            tellFriendStatus(JID("bb@localhost"))
                            tellFriendStatus(JID("cc@localhost"))


                        } else if (headPacket.isInstanceOf[Message]) {
                            val inMessage = headPacket.asInstanceOf[Message]
                            val from = inMessage.from.getOrElse(jid)
                            val to = inMessage.to.get
                            val idValue = inMessage.id.getOrElse("chat_1")
                            logger.debug(s"in message ${from.toString()} ${to.toString()} ${inMessage.body.getOrElse("")}")

                            val echoMessage = <message from={to.toString()} to={from.toString()} type="chat" id={idValue}>
                                <body>I know what you said: {inMessage.body.getOrElse("")}</body>
                            </message>

                            sender() ! Message(echoMessage)

                        } else {
                            logger.warn(s"unknown packet ${headPacket.toString()} duration status ${status}")
                        }

                }


                if (supported && packets.tail != null && !packets.tail.isEmpty) {
                    handlePackets(packets.tail)
                }
                if (supported == false) {
                    logger.error(s"cant process packet ${packets}")
                }

        }
    }
}


/*

if (packets.head.isInstanceOf[StreamHead]) {
                    val streamHead = packets.head.asInstanceOf[StreamHead]
                    val fromClient = streamHead.attributes.get("from").getOrElse("")
                    uid = fromClient.split("@")(0)

                    status match {
                        case Status.INIT =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            logger.debug(s"stream head ${streamHead}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val tls = <starttls xmlns={Tls.namespace}><optional/></starttls>

                            val mechanism = <mechanisms xmlns={Sasl.namespace}><mechanism>
                                {SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                                <optional/>
                            </mechanisms>

                            val features = Features(List(tls, mechanism))

                            sender() ! features
                        case EXPECT_SASL =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head ${streamHead} at ${status}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val mechanism = <mechanisms xmlns={Sasl.namespace}>
                                <mechanism>{SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                            </mechanisms>

                            val comprehension = <compression xmlns="http://jabber.org/features/compress">
                                <method>zlib</method>
                            </compression>

                            val features = Features(List(mechanism, comprehension))

                            sender() ! features

                        case EXPECT_NEW_STREAM2 =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "id" -> fromClient,
                                "version" -> "1.0")
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head 2 ${streamHead}")

                            sender() ! XmlHead()

                            sender() ! responseHead

                            val compression = <compression xmlns="http://jabber.org/features/compress">
                                <method>zlib</method>
                            </compression>
                            val bind = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/>
                            val session = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
                            val register = <register xmlns='http://jabber.org/features/iq-register'/>
                            val features = Features(List(compression, bind, session, register))
                            sender() ! features

                            debug("accept new stream 2 and response features")

                        //this cant be received by client, why?

                        case _ =>
                            warn("what status ? ")
                    }


                } else if (packets.head.isInstanceOf[StartTls]) {
                    val proceed = TlsProceed()
                    sender() ! proceed
                    status = EXPECT_SASL
                } else if (packets.head.isInstanceOf[SaslAuth]) {

                    val auth = packets.head.asInstanceOf[SaslAuth]
                    auth.mechanism match {
                        case SaslMechanism.Plain =>
                            debug("plain auth")
                            sender() ! SaslSuccess(auth.value)
                            status = EXPECT_NEW_STREAM2

                        case SaslMechanism.DiagestMD5 =>
                            debug("digest md5 auth")
                            val (_, response) = saslServer.evaluateResponse(null)
                            val challengeBase64 = (new BASE64Encoder()).encode(response).replace("\r","").replace("\n","")
                            debug(s"challengeBase64 ${challengeBase64}")

                            val challenge = SaslChallenge(challengeBase64)

                            sender() ! challenge
                        case _ =>
                            warn("unknown mechanism")

                    }
                } else if (packets.head.isInstanceOf[SaslResponse]) {
                    val response = packets.head.asInstanceOf[SaslResponse]
                    debug(s"response text ${response.responseText}")

                    val decodedResponseBytes = (new BASE64Decoder()).decodeBuffer(response.responseText)
                    val decodedResponseText = new String(decodedResponseBytes)
                    debug(s"decoded responseText ${decodedResponseText}")
                    val (ok, challenge) = saslServer.evaluateResponse(decodedResponseBytes)
                    ok match {
                        case true =>
                            debug("sasl success")
                            val challengeBase64 = (new BASE64Encoder()).encode(challenge)
                            sender() ! SaslSuccess(challengeBase64)
                            status = EXPECT_NEW_STREAM2
                        case false =>
                            val challengeBase64 = (new BASE64Encoder()).encode(challenge).replace("\r","").replace("\n","")
                            debug(s"challengeBase64 again ${challengeBase64}")
                            sender() ! SaslChallenge(challengeBase64)
                    }
                } else if (packets.head.isInstanceOf[iq.IQ]) {
                    if (clientConnection == null) {
                        clientConnection = sender()
                    }
                    val iqStanza = packets.head.asInstanceOf[iq.IQ]


                    debug(s"iq ${iqStanza.toString}")

                    iqStanza match {
                        case get @ iq.Get(oid, oto, ofrom, ext) =>
                            debug(s"get ${get}")

                            val id = oid.getOrElse("")


                            val xml = <iq type='result' id={id}>
                                <query xmlns='jabber:iq:auth'>
                                    <username>aa</username>
                                    <password/>
                                    <digest/>
                                    <resource/>
                                </query>
                            </iq>

                            /*
                            val xml = <iq from='localhost' id={id} type='error'>
                                <error code='503' type='cancel'>
                                    <service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
                                </error>
                            </iq>
                            */
                            /*
                            val xml = <iq id={id} type="error">
                                <query xmlns="jabber:iq:auth">
                                    <username>aa</username>
                                </query>
                                <error code="405" type="CANCEL">
                                    <not-allowed xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
                                    <text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Connection must be encrypted.
                                    </text>
                                </error>
                            </iq>*/
                            val res = iq.Result(xml)
                            debug(s"response to iq get auth ${xml.toString()}")
                            sender() ! res

                        case set @ iq.Set(Some("auth_2"), oto, ofrom, ext) =>
                            val xml = <iq type='result' id='auth_2'/>
                            val res = iq.Result(xml)
                            debug(s"response to iq set auth ${xml.toString()}")
                            sender() ! res
                        case set @ iq.Set(Some(idstr), oto, ofrom, ext) =>
                            val xml = <iq type='result' id={idstr}/>
                            /*
                            val xml = <iq id={idstr} type="error">
                                <query xmlns="jabber:iq:auth">
                                    <username>aa</username>
                                </query>
                                <error code="405" type="CANCEL">
                                    <not-allowed xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
                                    <text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Connection must be encrypted.
                                    </text>
                                </error>
                            </iq>
                            */

                            ext match {
                                case Some(ext) =>
                                    debug(s"iq set ext ${ext}")
                                case None =>

                            }

                            val res = iq.Result(xml)
                            debug(s"response to iq set auth ${xml.toString()}")
                            sender() ! res
                        case _ =>
                            if (jid == null) {
                                ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.IqBind, uid, None, iqStanza)
                            } else {
                                ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.IqBind, uid, Some(jid), iqStanza)
                            }
                    }


                } else if (packets.head.isInstanceOf[message.Message]) {
                    if (jid == null) {
                        ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.MessageRequest, uid, None, packets.head)
                    } else {
                        ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.MessageRequest, uid, Some(jid), packets.head)
                    }

                } else {
                    supported = false
                    warn(s"unsupported packet now ${packets.head}")
                    sender() ! new UnexpectedPacketException("packet is not stream head")
                }

                if (supported && packets.tail != null && !packets.tail.isEmpty) {
                    handlePackets(packets.tail)
                }
 */
