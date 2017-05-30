package me.babaili.ajabberd.xmpp

import akka.actor.{Actor, ActorRef}
import me.babaili.ajabberd.auth.MySaslServer
import me.babaili.ajabberd.data.{Passport, Roster, VCardManager}
import me.babaili.ajabberd.protocol._
import me.babaili.ajabberd.util
import me.babaili.ajabberd.global.ApplicationContext
import me.babaili.ajabberd.protocol.extensions.{Query, VCard}
import me.babaili.ajabberd.protocol.extensions.message._
import me.babaili.ajabberd.protocol.message.Message
import me.babaili.ajabberd.util.{Logger, XMLUtil}
import me.babaili.ajabberd.xmpp.XmppEvents.{Command, IqBind, MessageRequest}
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.collection.immutable.HashMap
import scala.xml.{Elem, NodeSeq}


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
    var saslServer:MySaslServer = null
    var clientConnection: ActorRef = null
    var jid: JID = JID.EmptyJID
    var uid: String = ""

    def receive: Receive = {
        case packets: XmppPackets =>
            logger.debug(s"received packets ${packets.packets.length}")
            handlePackets(packets.packets)
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
                        supported = handlePacketsAtInit(headPacket)
                    case Status.AUTHENTICATION_NEGOTIATION =>
                        supported = handlePacketAtAuthenticating(headPacket)
                    case Status.EXPECT_SASL =>
                        supported = handlePacketAtSasl(headPacket)
                    case Status.EXPECT_NEW_SESSION =>
                        supported = handlePacketAtNewSession(headPacket)
                    case Status.PREPARE_TALK =>
                        supported = handlePacketAtTalking(headPacket)
                }


                if (supported && packets.tail != null && !packets.tail.isEmpty) {
                    handlePackets(packets.tail)
                }
                if (supported == false) {
                    logger.error(s"at status ${status} cant process packet ${packets}")
                }

        }
    }

    def handlePacketsAtInit(headPacket: Packet): Boolean = {

        var supported: Boolean = true
        logger.debug(s"head xml ${headPacket.toString()}")

        if (headPacket.isInstanceOf[StreamHead]) {
            val streamHead = headPacket.asInstanceOf[StreamHead]
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

        supported

    }


    def handlePacketAtAuthenticating(headPacket: Packet): Boolean = {
        var supported: Boolean = true

        if (headPacket.isInstanceOf[StartTls]) {
            val proceed = TlsProceed()
            sender() ! proceed
            status = Status.EXPECT_SASL
        } else if (headPacket.isInstanceOf[iq.IQ]) {
            if (clientConnection == null) {
                clientConnection = sender()
            }
            val iqStanza = headPacket.asInstanceOf[iq.IQ]


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
            supported  = false
            logger.warn(s"unknown packet duration status ${status}")
        }

        supported
    }

    def handlePacketAtSasl(headPacket: Packet): Boolean = {
        var supported: Boolean = true

        if (headPacket.isInstanceOf[StreamHead]) {
            val streamHead = headPacket.asInstanceOf[StreamHead]
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


            val features = Features(List(mechanism))

            sender() ! features
        } else if (headPacket.isInstanceOf[SaslAuth]) {
            val auth = headPacket.asInstanceOf[SaslAuth]
            auth.mechanism match {
                case SaslMechanism.Plain =>
                    val decodedBytes = (new BASE64Decoder()).decodeBuffer(auth.value)

                    decodedBytes.isEmpty match {
                        case true =>
                            sender() ! SaslError(<invalid-authzid/>)
                        case false =>
                            val decodedString = new String(decodedBytes, "UTF-8")
                            decodedBytes.foreach(b => println(s"bbbbbbb ==> ${b}"))
                            logger.debug(s"plain auth ${decodedBytes.length} ${decodedString}")
                            import java.util.StringTokenizer
                            val tokens = new StringTokenizer(decodedString, "\0")
                            if (tokens.countTokens() >= 2) {
                                val user = tokens.nextToken()
                                val password = tokens.nextToken()
                                if (Passport.validate(user, password)) {
                                    uid = user
                                    Passport.queryUserJid(uid).foreach(x => jid = JID(x + "@localhost"))
                                    logger.debug(s"sasl success ${uid} ${jid.toString}")
                                    status = Status.EXPECT_NEW_SESSION
                                    sender() ! SaslSuccess(auth.value)
                                } else {
                                    sender() ! SaslError(<invalid-authzid/>)
                                }
                            } else {
                                sender() ! SaslError(<invalid-authzid/>)
                            }
                    }
                case SaslMechanism.DiagestMD5 =>
                    logger.debug("digest md5 auth")
                    saslServer = new MySaslServer("DIGEST-MD5")
                    val (_, response) = saslServer.evaluateResponse(null)
                    val challengeBase64 = (new BASE64Encoder()).encode(response).replace("\r","").replace("\n","")
                    logger.debug(s"challengeBase64 ${challengeBase64}")

                    val challenge = SaslChallenge(challengeBase64)

                    sender() ! challenge
                case _ =>
                    logger. warn("unknown mechanism")

            }

        } else if (headPacket.isInstanceOf[SaslResponse]) {
            val response = headPacket.asInstanceOf[SaslResponse]
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
            supported = false
            logger.warn(s"unknown packet duration status ${status}")
        }

        supported
    }

    def handlePacketAtNewSession(headPacket: Packet): Boolean = {
        var supported: Boolean = true

        if (headPacket.isInstanceOf[StreamHead]) {
            val streamHead = headPacket.asInstanceOf[StreamHead]
            val fromClient = streamHead.attributes.get("from").getOrElse("")

            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                "id" -> fromClient,
                "version" -> "1.0")
            val responseHead = StreamHead("jabber:client", responseAttributes)
            logger.debug(s"new session ${streamHead}")

            sender() ! XmlHead()

            sender() ! responseHead

            val bind = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/>
            val session = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
            val register = <register xmlns='http://jabber.org/features/iq-register'/>
            val features = Features(List(bind, session, register))
            sender() ! features

            status = Status.PREPARE_TALK
        } else {
            supported = false
            logger.warn(s"unknown packet duration status ${status}")
        }

        supported
    }

    def handlePacketAtTalking(headPacket: Packet): Boolean = {
        var supported: Boolean = true

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
                                <jid>{jid.toString}</jid>
                            </bind>
                        </iq>

                        sender() ! iq.Result(xml)

                        ApplicationContext.getXmppServer() ! Command(IqBind,uid,Some(jid),headPacket)

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
                    } else if (extension.isInstanceOf[extensions.VCard]) {
                        //
                        val vCard = extension.asInstanceOf[VCard]
                        logger.info(s"iq set for vcard ${vCard}")
                        val idValue = oId.getOrElse("bind_2")
                        val xml = <iq from='localhost' type='result' id={idValue}/>

                        sender() ! iq.Result(xml)
                    } else {
                        logger.warn(s"cant deal with iq set duration status ${status}")
                    }
                case get@iq.Get(oId, oTo, oFrom, Some(extension)) =>
                    if (extension.isInstanceOf[extensions.Query]) {
                        val query = extension.asInstanceOf[extensions.Query]
                        query.namespace.getOrElse("") match {
                            case "http://jabber.org/protocol/disco#items" =>
                                val idValue = oId.getOrElse("disco_1")
                                val toJid = oFrom.getOrElse(jid).toString
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
                case get @ iq.Get(oId, oTo, oFrom, Some(extension)) =>

                    extension match {
                        case query: Query =>
                            val ns = query.namespace.getOrElse("")
                            ns match {
                                case "http://jabber.org/protocol/disco#items" =>
                                    val idValue = oId.getOrElse("")
                                    val toJid = oFrom.getOrElse(jid).toString

                                    val xml = <iq from='localhost' type='result' id={idValue} to={toJid}>
                                        <query xmlns='http://jabber.org/protocol/disco#items'>
                                            <item jid='localhost'/>
                                        </query>
                                    </iq>

                                    sender() ! iq.Result(xml)
                                case "jabber:iq:roster" =>
                                    val idValue = oId.getOrElse("")
                                    val toJid = oFrom.getOrElse(jid).toString

                                    var parent:Elem = <query xmlns="jabber:iq:roster"></query>
                                    val friends = Roster.getFriends(uid)
                                    if (!friends.isEmpty) {
                                        friends.get.foreach(friend => parent = XMLUtil.addChild(parent,
                                            <item jid={friend.toString} name={friend.node} subscription="both"><group>Friends</group></item>))
                                    }
                                    val xml = <iq id={idValue} type="result" to={toJid.toString}>
                                        {parent}
                                    </iq>

                                    logger.debug(s"hi ${uid} your friends ${xml.toString()}")
                                    sender() ! iq.Result(xml)
                                case "http://jabber.org/protocol/disco#info" =>
                                    //xep-0030
                                    val idValue = oId.getOrElse("")
                                    val toJid = oFrom.getOrElse(jid).toString

                                    val xml = <iq type='result' from='localhost' to={toJid} id={idValue}>
                                        <query xmlns='http://jabber.org/protocol/disco#info'>
                                            <identity
                                            category="server" type="im" name="Tigase ver. 0.0.0-0"/>
                                            <identity
                                            category='conference' type='text' name='Play-Specific Chatrooms'/>
                                            <identity
                                            category='directory' type='chatroom' name='Play-Specific Chatrooms'/>
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
                                case extensions.get.PrivateQuery.namespace =>
                                    val result = iq.Result(Some(oId.getOrElse("private_1")),
                                        Some(oFrom.getOrElse(jid)),
                                        Some(JID("@localhost")),
                                        Some(extension))
                                    sender() ! result
                                case extensions.get.LastQuery.namespace =>
                                    val xml = <iq
                                    from='localhost'
                                    id={oId.getOrElse("last_1")}
                                    to={oFrom.getOrElse(jid).toString()}
                                    type='result'>
                                        <query xmlns='jabber:iq:last' seconds='903'>Heading Home</query>
                                    </iq>

                                    logger.debug("https://xmpp.org/extensions/xep-0012.html")
                                    sender() ! iq.Result(xml)
                                case "" =>
                                    logger.warn("??? empty namespace")
                                case x =>
                                    logger.error(s"??? unknown namespace ${x} of iq get on status ${status}")

                                    sender() ! iq.Result(oId,oTo,oFrom,None)
                            }
                        case vcard: VCard =>
                            ///
                            logger.info(s"******************** vcard ${vcard}")
                            val idValue = oId.getOrElse("v1")
                            val fromJid = oTo.getOrElse(jid)
                            val xml = <iq id={idValue} to={jid.toString} type='result'></iq>
                            val vCard = VCardManager.getVcard(fromJid.node)
                            vCard.isEmpty match {
                                case true =>
                                    sender() ! iq.Result(xml)
                                case false =>
                                    sender() ! iq.Result(util.XMLUtil.addChild(xml, vCard.get))
                            }
                        case x: Any =>
                            logger.warn(s"can't understand the get extension ${x}")
                    }



                case get @ iq.Get(oId, oTo, oFrom, None) =>
                    //example
                    //<iq type="get" from="aa@localhost" to="localhost" id="rvTo2-26"><services xmlns="http://jabber.org/protocol/jinglenodes"/></iq>
                    logger.warn("??? empty extension")

                    //we might need to build more extension class
                    val headString = headPacket.toString()
                    logger.debug(s"head string ${headString}")

                    val idValue = oId.getOrElse("v1")
                    val fromJid = oFrom.getOrElse(jid)
                    if (headString.indexOf("vCard") > 0) {
                        val xml = <iq id={idValue} from={fromJid.toString} to={jid.toString} type='result'></iq>
                        val vCard = VCardManager.getVcard(fromJid.node)
                        vCard.isEmpty match {
                            case true =>
                                sender() ! iq.Result(xml)
                            case false =>
                                sender() ! iq.Result(util.XMLUtil.addChild(xml, vCard.get))
                        }

                    } else if (headString.indexOf("jabber:iq:last") > 0) {
                        //https://xmpp.org/extensions/xep-0012.html
                        val idValue = oId.getOrElse("last_1")
                        val xml = <iq from='juliet@capulet.com'
                                      id={idValue}
                                      to={oFrom.getOrElse(jid).toString()}
                                      type='result'>
                            <query xmlns='jabber:iq:last' seconds='903'>Heading Home</query>
                        </iq>

                        logger.debug("https://xmpp.org/extensions/xep-0012.html")
                        sender() ! iq.Result(xml)
                    } else if (headString.indexOf("http://www.jivesoftware.org/protocol/sharedgroup") > 0) {
                        val xml = <iq type="result" id={oId.getOrElse("")} to={oFrom.getOrElse(jid).toString()}>
                            <sharedgroup xmlns="http://www.jivesoftware.org/protocol/sharedgroup"/>
                        </iq>
                        sender() ! iq.Result(xml)
                    } else {
                        val xml = <iq from="localhost"
                                      to={oFrom.getOrElse(jid).toString()}
                                      id={idValue} type="result"/>
                        sender() ! iq.Result(xml)
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
                    <status>Online</status>
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

            inMessage.extensions.isEmpty match {
                case true =>
                    val echoMessage = <message from={to.toString()} to={from.toString()} type="chat" id={idValue}>
                        <body>I know what you said:{inMessage.body.getOrElse("")}</body>
                        <thread>{inMessage.thread.getOrElse("")}</thread>
                        <active xmlns='http://jabber.org/protocol/chatstates'/>
                    </message>

                    sender() ! Message(echoMessage)

                    ApplicationContext.getXmppServer() ! Command(MessageRequest, uid, Some(from), headPacket)
                case false =>
                    inMessage.extensions.get.head match {
                        case ActiveState(xml) =>
                            val echoMessage = <message from={to.toString()} to={from.toString()} type="chat" id={idValue}>
                                <body>I know what you said:{inMessage.body.getOrElse("")}</body>
                                <thread>{inMessage.thread.getOrElse("")}</thread>
                                <active xmlns='http://jabber.org/protocol/chatstates'/>
                            </message>

                            sender() ! Message(echoMessage)

                            ApplicationContext.getXmppServer() ! Command(MessageRequest, uid, Some(from), headPacket)
                        case ComposingState(xml) =>
                            logger.debug(s"{}{}{}{}{} composing ${xml.text}")
                        case GoneState(xml) =>
                            logger.debug(s"{}{}{}{}{} gone ${xml.text}")
                        case InactiveState(xml) =>
                            logger.debug(s"{}{}{}{}{} inactive ${xml.text}")
                        case PausedState(xml) =>
                            logger.debug(s"{}{}{}{}{} paused ${xml.text}")

                    }
            }
        } else if(headPacket.isInstanceOf[SaslAuth]) {
            logger.warn("saal auth? why ?????")
            sender() ! SaslSuccess()
        } else {
            supported = false
            logger.warn(s"unknown packet ${headPacket.toString()} duration status ${status}")
        }

        supported

    }
}