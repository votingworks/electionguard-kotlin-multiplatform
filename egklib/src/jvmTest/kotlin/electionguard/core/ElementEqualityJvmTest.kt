package electionguard.core

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ElementEqualityJvmTest {

    /** Test ElementModQ compares equal even when BigInteger is unnormalized. */
    @Test
    fun testElementModQ() {
        val group = productionGroup() as ProductionGroupContext
        val bi = BigInteger("89378920920032937196531702992192972263302712977973574040976517358784464109329")
        val biq: ElementModQ = ProductionElementModQ(bi, group)
        val biu: UInt256 = biq.toUInt256()
        assertEquals(biu.toString(), "UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)")

        val bi2 = BigInteger("-26413168317283258227039282016494935589967271687666989998481066649128665530607")
        val biq2: ElementModQ = ProductionElementModQ(bi2, group)
        val biu2: UInt256 = biq2.toUInt256()
        assertEquals(biu2.toString(), "UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)")

        assertEquals(biu2, biu)
        assertEquals(biq2.base16(), biq.base16())

        // ElementModQ now equal because it uses normalized bytes
        assertNotEquals(bi2, bi)
        assertEquals(biq2, biq)
        assertEquals(biq2.hashCode(), biq.hashCode())
        assertEquals(biq2.toString(), biq.toString())
    }

    /** Test ElementModP compares equal even when BigInteger is unnormalized. */
    @Test
    fun testElementModP() {
        val group = productionGroup() as ProductionGroupContext
        val big10 = BigInteger(
            "-985885428196823793871112996193692117877338077477509313481176490080043572707575411869476082314071793964095333398744714332767945309577734923855959666952312525705983935794224254844921191506542799980986277785730101641969998008950665607552674779780255443794340755631453736511195965846294137817959149387975188426988363938995046173983376227628568878079936975107253562158738016217863695915516514543524467023351623372147699587151823964788878931625535092111399233104855398144355716461292664016469938048164770562262915176131141149306974339848374124423701816561022798560781880074347370789098569941039608169169423225225700784888687065727520873473846824836442424518979566061683638437879690405121721580003679584987758993406983880687066931891663303142191385688382810398036456017524079326674174907842832390598151860062729968684600437956686678947144020988482631829670396480266255416655992443923337019632004174957302256795150925344684757543832824644645093832263068544636807355769443006223707566815533908185857489241864957258108438753697424938535863201731916633015843613633242770037047355829251893057397651915198708984643944247423365612497135095192340593592328347935363489421152889044708789452933119622244096831912473291882094524612876636835909701347"
        )
        val big16 = BigInteger(
            "-3ddd686fd551ed14a405be11987688a4750100ef48f3ff5088877298277d5a5fe29d6a7e544c4cf68226578fce361f184c4c1e9a3b445869f2233114fd836e1b1cb689fe3b558b3c0258199c09e04788c616039c38c1cc60629b2142a19d3b12f9a48a8b232b9e9081221a0eadecbf4d76f9655100237e8587b220b19ed33ca73c980ce0883f70dbe36c244ba8b6738a5cca6d90f3d78ec6e5eae1d6c6f0878fd28f629fde2dcb4b9869364d21b0f0c7f484721a9daf3d4d193023f8ff220b384faf19531dea8301f68c5a7142e455af7a969f486c9ca106b54281d5c3c3e0d61ef2badffae3d8c2d77a96c82f2fceff809f64df23646ee3d2d1962475a89aacd7bc1eb12e3eafd828b8a29912fa4340b481b9f571f11aa3df73d96ffc49e6ed41ea32fd9523854476568eb20fbbfc2cd0ac7c7036cf684fb77a43bce4ad7f542ac4ea636bfd56442d92639eaf742be0ad2b8b19c5fa59f1a4cd3c844d434e33fda40569c9b34d18cb16d5f3446ab506ddc735d081d0c1cf1b37060da37b187dd7d3b9dc2f5890c084dcaeeae4d358118f5e414b2e2d2d01200c85ff18bc3e7b78be756fb8602eda65069f767d0ae459d36ae96f5d3b5191ed7b1b7f74de4146e364b7f5873fd366013a7fe2c1ad416628b78e23942996a4bac282713d5b82c3d05fe71f3c5809cc7aaffc3cdbb48367398b5e888f3dcde8ba1167c9fb8ee3",
            16
        )
        assertEquals(big10, big16)
        val p: ElementModP = ProductionElementModP(big10, group)

        val normalized = p.byteArray()
        val bign = BigInteger(1, normalized)
        val pn = ProductionElementModP(bign, group)

        assertNotEquals(big10, bign)
        assertEquals(p, pn)
        assertEquals(p.hashCode(), pn.hashCode())
        assertEquals(p.toString(), pn.toString())
    }

    /** Shows that BigIntegers's cant be normalized by normalizing the byte array in the constructor. */
    @Test
    fun testNormalizedBigIntegers() {
        val group = productionGroup() as ProductionGroupContext
        val q2 = group.TWO_MOD_Q
        val q2b: BigInteger = q2.element
        assertNotEquals(q2b.toByteArray().size, 32)

        val ba : ByteArray = q2.element.toByteArray()
        val ban = ba.normalize(32)
        assertEquals(ban.size, 32)
        val q2n = ProductionElementModQ(BigInteger(ban), group)

        assertEquals(q2, q2n)
        val q2nb: BigInteger = q2n.element
        assertNotEquals(q2nb.toByteArray().size, 32)

        assertEquals(q2.element.toString(), q2n.element.toString())
        assertEquals(q2.element, q2n.element)
    }
}