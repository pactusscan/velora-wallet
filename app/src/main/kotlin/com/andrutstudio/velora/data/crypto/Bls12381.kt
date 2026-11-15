package com.andrutstudio.velora.data.crypto

import java.math.BigInteger
import java.security.MessageDigest

/**
 * BLS12-381 signing for Pactus BLS accounts (pc1z), byte-compatible with
 * pactus-project/pactus crypto/bls:
 * - signature on G1 (48 bytes compressed), public key on G2 (96 bytes compressed),
 * - hash-to-curve BLS12381G1_XMD:SHA-256_SSWU_RO_ (RFC 9380),
 * - private key = 32-byte big-endian scalar reduced modulo the group order.
 *
 * Plain Kotlin on BigInteger: signing only needs hashing to G1 and scalar
 * multiplication, no pairing. The constants match @noble/curves, which the
 * browser extension uses, and the official Pactus vectors in Bls12381Test.
 */
object Bls12381 {
    val P = BigInteger("1a0111ea397fe69a4b1ba7b6434bacd764774b84f38512bf6730d2a0f6b0f6241eabfffeb153ffffb9feffffffffaaab", 16)
    val R = BigInteger("73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", 16)
    private val DST = "BLS_SIG_BLS12381G1_XMD:SHA-256_SSWU_RO_NUL_".toByteArray()
    private val HALF_P = P.shiftRight(1)
    private val SQRT_EXP = P.add(BigInteger.ONE).shiftRight(2) // p = 3 mod 4
    private val H_EFF_G1 = BigInteger("d201000000010001", 16)

    // ---- fields ----

    private interface Field<T> {
        val zero: T
        val one: T
        fun add(a: T, b: T): T
        fun sub(a: T, b: T): T
        fun mul(a: T, b: T): T
        fun inv(a: T): T
        fun isZero(a: T): Boolean
    }

    private object Fp : Field<BigInteger> {
        override val zero: BigInteger = BigInteger.ZERO
        override val one: BigInteger = BigInteger.ONE
        override fun add(a: BigInteger, b: BigInteger): BigInteger = a.add(b).mod(P)
        override fun sub(a: BigInteger, b: BigInteger): BigInteger = a.subtract(b).mod(P)
        override fun mul(a: BigInteger, b: BigInteger): BigInteger = a.multiply(b).mod(P)
        override fun inv(a: BigInteger): BigInteger = if (a.signum() == 0) a else a.modInverse(P)
        override fun isZero(a: BigInteger) = a.signum() == 0
    }

    /** a = c0 + c1 * i with i^2 = -1. */
    data class Fp2(val c0: BigInteger, val c1: BigInteger)

    private object Fp2Field : Field<Fp2> {
        override val zero = Fp2(BigInteger.ZERO, BigInteger.ZERO)
        override val one = Fp2(BigInteger.ONE, BigInteger.ZERO)
        override fun add(a: Fp2, b: Fp2) = Fp2(Fp.add(a.c0, b.c0), Fp.add(a.c1, b.c1))
        override fun sub(a: Fp2, b: Fp2) = Fp2(Fp.sub(a.c0, b.c0), Fp.sub(a.c1, b.c1))
        override fun mul(a: Fp2, b: Fp2) = Fp2(
            Fp.sub(a.c0.multiply(b.c0), a.c1.multiply(b.c1)),
            Fp.add(a.c0.multiply(b.c1), a.c1.multiply(b.c0)),
        )
        override fun inv(a: Fp2): Fp2 {
            val norm = Fp.inv(Fp.add(a.c0.multiply(a.c0), a.c1.multiply(a.c1)))
            return Fp2(Fp.mul(a.c0, norm), Fp.mul(P.subtract(a.c1).mod(P), norm))
        }
        override fun isZero(a: Fp2) = a.c0.signum() == 0 && a.c1.signum() == 0
    }

    // ---- points on y^2 = x^3 + b, Jacobian coordinates, z = 0 is infinity ----

    private class Point<T>(val x: T, val y: T, val z: T)

    private class Curve<T>(val f: Field<T>) {
        val infinity = Point(f.one, f.one, f.zero)

        fun double(p: Point<T>): Point<T> = with(f) {
            if (isZero(p.z) || isZero(p.y)) return infinity
            val a = mul(p.x, p.x)
            val b = mul(p.y, p.y)
            val c = mul(b, b)
            val xb = add(p.x, b)
            val d = twice(sub(sub(mul(xb, xb), a), c))
            val e = add(twice(a), a)
            val x3 = sub(mul(e, e), twice(d))
            val y3 = sub(mul(e, sub(d, x3)), twice(twice(twice(c))))
            val z3 = twice(mul(p.y, p.z))
            Point(x3, y3, z3)
        }

        fun add(p: Point<T>, q: Point<T>): Point<T> = with(f) {
            if (isZero(p.z)) return q
            if (isZero(q.z)) return p
            val z1z1 = mul(p.z, p.z)
            val z2z2 = mul(q.z, q.z)
            val u1 = mul(p.x, z2z2)
            val u2 = mul(q.x, z1z1)
            val s1 = mul(mul(p.y, q.z), z2z2)
            val s2 = mul(mul(q.y, p.z), z1z1)
            val h = sub(u2, u1)
            val r = twice(sub(s2, s1))
            if (isZero(h)) return if (isZero(r)) double(p) else infinity
            val i = mul(twice(h), twice(h))
            val j = mul(h, i)
            val v = mul(u1, i)
            val x3 = sub(sub(mul(r, r), j), twice(v))
            val y3 = sub(mul(r, sub(v, x3)), twice(mul(s1, j)))
            val zs = add(p.z, q.z)
            val z3 = mul(sub(sub(mul(zs, zs), z1z1), z2z2), h)
            Point(x3, y3, z3)
        }

        fun multiply(p: Point<T>, k: BigInteger): Point<T> {
            var acc = infinity
            for (bit in k.bitLength() - 1 downTo 0) {
                acc = double(acc)
                if (k.testBit(bit)) acc = add(acc, p)
            }
            return acc
        }

        /** Affine (x, y), or null for the point at infinity. */
        fun toAffine(p: Point<T>): Pair<T, T>? = with(f) {
            if (isZero(p.z)) return null
            val zInv = inv(p.z)
            val zInv2 = mul(zInv, zInv)
            Pair(mul(p.x, zInv2), mul(p.y, mul(zInv2, zInv)))
        }

        private fun twice(a: T) = f.add(a, a)
    }

    private val g1 = Curve(Fp)
    private val g2 = Curve(Fp2Field)

    private val G2_GENERATOR = Point(
        Fp2(
            BigInteger("024aa2b2f08f0a91260805272dc51051c6e47ad4fa403b02b4510b647ae3d1770bac0326a805bbefd48056c8c121bdb8", 16),
            BigInteger("13e02b6052719f607dacd3a088274f65596bd0d09920b61ab5da61bbdc7f5049334cf11213945d57e5ac7d055d042b7e", 16),
        ),
        Fp2(
            BigInteger("0ce5d527727d6e118cc9cdc6da2e351aadfd9baa8cbdd3a76d429a695160d12c923ac9cc3baca289e193548608b82801", 16),
            BigInteger("0606c4a02ea734cc32acd2b02bc28b99cb3e287e85a763af267492ab572e99ab3f370d275cec1da1aaa9075ff05f79be", 16),
        ),
        Fp2Field.one,
    )

    // ---- keys and signatures ----

    /** Same rule as bls.PrivateKeyFromBytes: reduce modulo the group order, reject zero. */
    fun scalarOf(privateKey: ByteArray): BigInteger {
        require(privateKey.size == 32) { "BLS private key must be 32 bytes" }
        val scalar = BigInteger(1, privateKey).mod(R)
        require(scalar.signum() != 0) { "Invalid BLS private key" }
        return scalar
    }

    fun normalizePrivateKey(privateKey: ByteArray): ByteArray = toBytes(scalarOf(privateKey), 32)

    /** 96-byte compressed G2 public key. */
    fun publicKey(privateKey: ByteArray): ByteArray {
        val (x, y) = g2.toAffine(g2.multiply(G2_GENERATOR, scalarOf(privateKey)))
            ?: error("Invalid BLS private key")
        val out = toBytes(x.c1, 48) + toBytes(x.c0, 48)
        out[0] = (out[0].toInt() or 0x80).toByte()
        val largest = if (y.c1.signum() != 0) y.c1 > HALF_P else y.c0 > HALF_P
        if (largest) out[0] = (out[0].toInt() or 0x20).toByte()
        return out
    }

    /** 48-byte compressed G1 signature over [message]. */
    fun sign(message: ByteArray, privateKey: ByteArray): ByteArray {
        val point = g1.multiply(hashToG1(message), scalarOf(privateKey))
        val (x, y) = g1.toAffine(point) ?: error("Signature is the point at infinity")
        val out = toBytes(x, 48)
        out[0] = (out[0].toInt() or 0x80).toByte()
        if (y > HALF_P) out[0] = (out[0].toInt() or 0x20).toByte()
        return out
    }

    // ---- hash to G1 (RFC 9380, suite BLS12381G1_XMD:SHA-256_SSWU_RO_) ----

    private fun hashToG1(message: ByteArray): Point<BigInteger> {
        val uniform = expandMessageXmd(message, DST, 128)
        val u0 = BigInteger(1, uniform.copyOfRange(0, 64)).mod(P)
        val u1 = BigInteger(1, uniform.copyOfRange(64, 128)).mod(P)
        val q = g1.add(mapToG1(u0), mapToG1(u1))
        return g1.multiply(q, H_EFF_G1)
    }

    internal fun expandMessageXmd(message: ByteArray, dst: ByteArray, length: Int): ByteArray {
        val sha = { data: ByteArray -> MessageDigest.getInstance("SHA-256").digest(data) }
        val ell = (length + 31) / 32
        val dstPrime = dst + byteArrayOf(dst.size.toByte())
        val b0 = sha(ByteArray(64) + message + byteArrayOf((length shr 8).toByte(), length.toByte(), 0) + dstPrime)
        var bi = sha(b0 + byteArrayOf(1) + dstPrime)
        var out = bi
        for (i in 2..ell) {
            val mixed = ByteArray(32) { (b0[it].toInt() xor bi[it].toInt()).toByte() }
            bi = sha(mixed + byteArrayOf(i.toByte()) + dstPrime)
            out += bi
        }
        return out.copyOf(length)
    }

    // Simplified SWU on the 11-isogenous curve E': y^2 = x^3 + A'x + B', Z = 11.
    private val ISO_A = BigInteger("144698a3b8e9433d693a02c96d4982b0ea985383ee66a8d8e8981aefd881ac98936f8da0e0f97f5cf428082d584c1d", 16)
    private val ISO_B = BigInteger("12e2908d11688030018b12e8753eee3b2016c1f0f24f4070a0b9c14fcef35ef55a23215a316ceaa5d1cc48e98e172be0", 16)
    private val SWU_Z = BigInteger.valueOf(11)

    private fun mapToG1(u: BigInteger): Point<BigInteger> {
        val (x, y) = sswu(u)
        val (ix, iy) = isoMap(x, y)
        return Point(ix, iy, BigInteger.ONE)
    }

    private fun sswu(u: BigInteger): Pair<BigInteger, BigInteger> = with(Fp) {
        val zu2 = mul(SWU_Z, mul(u, u))
        val tv1 = inv(add(mul(zu2, zu2), zu2))
        val x1 = if (isZero(tv1)) {
            mul(ISO_B, inv(mul(SWU_Z, ISO_A)))
        } else {
            mul(mul(P.subtract(ISO_B), inv(ISO_A)), add(one, tv1))
        }
        val gx1 = add(mul(mul(x1, x1), x1), add(mul(ISO_A, x1), ISO_B))
        val x2 = mul(zu2, x1)
        val gx2 = add(mul(mul(x2, x2), x2), add(mul(ISO_A, x2), ISO_B))
        val sqrt1 = gx1.modPow(SQRT_EXP, P)
        val (x, yy) = if (mul(sqrt1, sqrt1) == gx1) x1 to sqrt1 else x2 to gx2.modPow(SQRT_EXP, P)
        val y = if (u.testBit(0) != yy.testBit(0)) P.subtract(yy).mod(P) else yy
        x to y
    }

    private fun isoMap(x: BigInteger, y: BigInteger): Pair<BigInteger, BigInteger> {
        // Horner evaluation of a polynomial in x
        fun polyAt(coefficients: List<BigInteger>): BigInteger =
            coefficients.foldRight(BigInteger.ZERO) { c, acc -> Fp.add(Fp.mul(acc, x), c) }
        val xNum = polyAt(ISO_X_NUM)
        val xDen = polyAt(ISO_X_DEN)
        val yNum = polyAt(ISO_Y_NUM)
        val yDen = polyAt(ISO_Y_DEN)
        return Fp.mul(xNum, Fp.inv(xDen)) to Fp.mul(y, Fp.mul(yNum, Fp.inv(yDen)))
    }

    // 11-isogeny map coefficients (RFC 9380 appendix E.2), constant term first.
    private fun hex(vararg values: String) = values.map { BigInteger(it, 16) }

    private val ISO_X_NUM = hex(
        "11a05f2b1e833340b809101dd99815856b303e88a2d7005ff2627b56cdb4e2c85610c2d5f2e62d6eaeac1662734649b7",
        "17294ed3e943ab2f0588bab22147a81c7c17e75b2f6a8417f565e33c70d1e86b4838f2a6f318c356e834eef1b3cb83bb",
        "d54005db97678ec1d1048c5d10a9a1bce032473295983e56878e501ec68e25c958c3e3d2a09729fe0179f9dac9edcb0",
        "1778e7166fcc6db74e0609d307e55412d7f5e4656a8dbf25f1b33289f1b330835336e25ce3107193c5b388641d9b6861",
        "e99726a3199f4436642b4b3e4118e5499db995a1257fb3f086eeb65982fac18985a286f301e77c451154ce9ac8895d9",
        "1630c3250d7313ff01d1201bf7a74ab5db3cb17dd952799b9ed3ab9097e68f90a0870d2dcae73d19cd13c1c66f652983",
        "d6ed6553fe44d296a3726c38ae652bfb11586264f0f8ce19008e218f9c86b2a8da25128c1052ecaddd7f225a139ed84",
        "17b81e7701abdbe2e8743884d1117e53356de5ab275b4db1a682c62ef0f2753339b7c8f8c8f475af9ccb5618e3f0c88e",
        "80d3cf1f9a78fc47b90b33563be990dc43b756ce79f5574a2c596c928c5d1de4fa295f296b74e956d71986a8497e317",
        "169b1f8e1bcfa7c42e0c37515d138f22dd2ecb803a0c5c99676314baf4bb1b7fa3190b2edc0327797f241067be390c9e",
        "10321da079ce07e272d8ec09d2565b0dfa7dccdde6787f96d50af36003b14866f69b771f8c285decca67df3f1605fb7b",
        "6e08c248e260e70bd1e962381edee3d31d79d7e22c837bc23c0bf1bc24c6b68c24b1b80b64d391fa9c8ba2e8ba2d229",
    )
    private val ISO_X_DEN = hex(
        "8ca8d548cff19ae18b2e62f4bd3fa6f01d5ef4ba35b48ba9c9588617fc8ac62b558d681be343df8993cf9fa40d21b1c",
        "12561a5deb559c4348b4711298e536367041e8ca0cf0800c0126c2588c48bf5713daa8846cb026e9e5c8276ec82b3bff",
        "b2962fe57a3225e8137e629bff2991f6f89416f5a718cd1fca64e00b11aceacd6a3d0967c94fedcfcc239ba5cb83e19",
        "3425581a58ae2fec83aafef7c40eb545b08243f16b1655154cca8abc28d6fd04976d5243eecf5c4130de8938dc62cd8",
        "13a8e162022914a80a6f1d5f43e7a07dffdfc759a12062bb8d6b44e833b306da9bd29ba81f35781d539d395b3532a21e",
        "e7355f8e4e667b955390f7f0506c6e9395735e9ce9cad4d0a43bcef24b8982f7400d24bc4228f11c02df9a29f6304a5",
        "772caacf16936190f3e0c63e0596721570f5799af53a1894e2e073062aede9cea73b3538f0de06cec2574496ee84a3a",
        "14a7ac2a9d64a8b230b3f5b074cf01996e7f63c21bca68a81996e1cdf9822c580fa5b9489d11e2d311f7d99bbdcc5a5e",
        "a10ecf6ada54f825e920b3dafc7a3cce07f8d1d7161366b74100da67f39883503826692abba43704776ec3a79a1d641",
        "95fc13ab9e92ad4476d6e3eb3a56680f682b4ee96f7d03776df533978f31c1593174e4b4b7865002d6384d168ecdd0a",
        "1",
    )
    private val ISO_Y_NUM = hex(
        "90d97c81ba24ee0259d1f094980dcfa11ad138e48a869522b52af6c956543d3cd0c7aee9b3ba3c2be9845719707bb33",
        "134996a104ee5811d51036d776fb46831223e96c254f383d0f906343eb67ad34d6c56711962fa8bfe097e75a2e41c696",
        "cc786baa966e66f4a384c86a3b49942552e2d658a31ce2c344be4b91400da7d26d521628b00523b8dfe240c72de1f6",
        "1f86376e8981c217898751ad8746757d42aa7b90eeb791c09e4a3ec03251cf9de405aba9ec61deca6355c77b0e5f4cb",
        "8cc03fdefe0ff135caf4fe2a21529c4195536fbe3ce50b879833fd221351adc2ee7f8dc099040a841b6daecf2e8fedb",
        "16603fca40634b6a2211e11db8f0a6a074a7d0d4afadb7bd76505c3d3ad5544e203f6326c95a807299b23ab13633a5f0",
        "4ab0b9bcfac1bbcb2c977d027796b3ce75bb8ca2be184cb5231413c4d634f3747a87ac2460f415ec961f8855fe9d6f2",
        "987c8d5333ab86fde9926bd2ca6c674170a05bfe3bdd81ffd038da6c26c842642f64550fedfe935a15e4ca31870fb29",
        "9fc4018bd96684be88c9e221e4da1bb8f3abd16679dc26c1e8b6e6a1f20cabe69d65201c78607a360370e577bdba587",
        "e1bba7a1186bdb5223abde7ada14a23c42a0ca7915af6fe06985e7ed1e4d43b9b3f7055dd4eba6f2bafaaebca731c30",
        "19713e47937cd1be0dfd0b8f1d43fb93cd2fcbcb6caf493fd1183e416389e61031bf3a5cce3fbafce813711ad011c132",
        "18b46a908f36f6deb918c143fed2edcc523559b8aaf0c2462e6bfe7f911f643249d9cdf41b44d606ce07c8a4d0074d8e",
        "b182cac101b9399d155096004f53f447aa7b12a3426b08ec02710e807b4633f06c851c1919211f20d4c04f00b971ef8",
        "245a394ad1eca9b72fc00ae7be315dc757b3b080d4c158013e6632d3c40659cc6cf90ad1c232a6442d9d3f5db980133",
        "5c129645e44cf1102a159f748c4a3fc5e673d81d7e86568d9ab0f5d396a7ce46ba1049b6579afb7866b1e715475224b",
        "15e6be4e990f03ce4ea50b3b42df2eb5cb181d8f84965a3957add4fa95af01b2b665027efec01c7704b456be69c8b604",
    )
    private val ISO_Y_DEN = hex(
        "16112c4c3a9c98b252181140fad0eae9601a6de578980be6eec3232b5be72e7a07f3688ef60c206d01479253b03663c1",
        "1962d75c2381201e1a0cbd6c43c348b885c84ff731c4d59ca4a10356f453e01f78a4260763529e3532f6102c2e49a03d",
        "58df3306640da276faaae7d6e8eb15778c4855551ae7f310c35a5dd279cd2eca6757cd636f96f891e2538b53dbf67f2",
        "16b7d288798e5395f20d23bf89edb4d1d115c5dbddbcd30e123da489e726af41727364f2c28297ada8d26d98445f5416",
        "be0e079545f43e4b00cc912f8228ddcc6d19c9f0f69bbb0542eda0fc9dec916a20b15dc0fd2ededda39142311a5001d",
        "8d9e5297186db2d9fb266eaac783182b70152c65550d881c5ecd87b6f0f5a6449f38db9dfa9cce202c6477faaf9b7ac",
        "166007c08a99db2fc3ba8734ace9824b5eecfdfa8d0cf8ef5dd365bc400a0051d5fa9c01a58b1fb93d1a1399126a775c",
        "16a3ef08be3ea7ea03bcddfabba6ff6ee5a4375efa1f4fd7feb34fd206357132b920f5b00801dee460ee415a15812ed9",
        "1866c8ed336c61231a1be54fd1d74cc4f9fb0ce4c6af5920abc5750c4bf39b4852cfe2f7bb9248836b233d9d55535d4a",
        "167a55cda70a6e1cea820597d94a84903216f763e13d87bb5308592e7ea7d4fbc7385ea3d529b35e346ef48bb8913f55",
        "4d2f259eea405bd48f010a01ad2911d9c6dd039bb61a6290e591b36e636a5c871a5c29f4f83060400f8b49cba8f6aa8",
        "accbb67481d033ff5852c1e48c50c477f94ff8aefce42d28c0f9a88cea7913516f968986f7ebbea9684b529e2561092",
        "ad6b9514c767fe3c3613144b45f1496543346d98adf02267d5ceef9a00d9b8693000763e3b90ac11e99b138573345cc",
        "2660400eb2e4f3b628bdd0d53cd76f2bf565b94e72927c1cb748df27942480e420517bd8714cc80d1fadc1326ed06f7",
        "e0fa1d816ddc03e6b24255e0d7819c171c40f65e273b853324efcd6356caa205ca2f570f13497804415473a1d634b8f",
        "1",
    )

    /** Big-endian, left-padded to [size] bytes. */
    fun toBytes(n: BigInteger, size: Int): ByteArray {
        val raw = n.toByteArray().let { if (it.size > size) it.copyOfRange(it.size - size, it.size) else it }
        return ByteArray(size - raw.size) + raw
    }
}
