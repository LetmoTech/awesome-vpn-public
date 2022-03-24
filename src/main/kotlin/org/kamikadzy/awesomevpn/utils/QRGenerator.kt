package org.kamikadzy.awesomevpn.utils

import io.nayuki.qrcodegen.QrCode
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class QRGenerator {
    fun getCodeForConfig(config: String): BufferedImage {
        val qrCode = QrCode.encodeText(config, QrCode.Ecc.HIGH)

        return toImage(qrCode)
    }

    private fun toImage(qr: QrCode, scale: Int = 10, border: Int = 4, lightColor: Int = 0xFFFFFF, darkColor: Int = 0x000000): BufferedImage {
         val result =
            BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val color = qr.getModule(x / scale - border, y / scale - border)
                result.setRGB(x, y, if (color) darkColor else lightColor)
            }
        }
        return result
    }

}