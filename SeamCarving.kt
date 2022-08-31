package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

fun gradient(horizontal: Boolean, x: Int, y: Int, image: BufferedImage): Double {
    val pixelA = Color(image.getRGB(if (horizontal) x - 1 else x, if (!horizontal) y - 1 else y))
    val pixelB = Color(image.getRGB(if (horizontal) x + 1 else x, if (!horizontal) y + 1 else y))
    return (pixelB.red - pixelA.red).toDouble().pow(2.0) + (pixelB.green - pixelA.green).toDouble().pow(2.0) + (pixelB.blue - pixelA.blue).toDouble().pow(2.0)
}

fun BufferedImage.energies(): Array<Array<Pair<Double, Int>?>> {
    val retval=Array(this.width) { arrayOfNulls<Pair<Double,Int>>(this.height)  }
    for(x in 0 until this.width)
        for(y in 0 until this.height)
            retval[x][y] = Pair(sqrt(gradient(true, x.coerceIn(1, this.width - 2), y, this) + gradient(false, x, y.coerceIn(1, this.height - 2), this)), 0)
    return retval
}

fun BufferedImage.rotate90(clockwise: Boolean = true): BufferedImage {
    val rotatedImage: BufferedImage = BufferedImage(this.height, this.width, this.type)
    for (i in 0 until this.width) for (j in 0 until this.height)
        if(clockwise) rotatedImage.setRGB(this.height - j - 1, i, this.getRGB(i, j))
        else rotatedImage.setRGB(j, this.width - i - 1, this.getRGB(i, j))
    return rotatedImage
}

fun main(args:Array<String>) {
    val infile = args[1]
    val outfile = args[3]
    val width = args[5].toInt()
    val height = args[7].toInt()

    var image = ImageIO.read(File(infile))

    for(i in 1..width)
        image = carve(image)

    image = image.rotate90(false)

    for(i in 1..height)
        image = carve(image)

    ImageIO.write(image.rotate90(), "png", File(outfile) );
}

fun carve(image: BufferedImage): BufferedImage {
    val outImage = BufferedImage(image.width-1, image.height, image.type)
    val energies = image!!.energies()

    for(y in 1 until image.height)
        for(x in 0 until image.width) {
            if(energies[max(0,x-1)][y-1]!!.first <= min(energies[x][y-1]!!.first, energies[min(image.width-1,x+1)][y-1]!!.first))
                energies[x][y] = Pair(energies[x][y]!!.first + energies[max(0,x-1)][y-1]!!.first, max(0,x-1))
            else if(energies[x][y-1]!!.first <= min(energies[max(0,x-1)][y-1]!!.first, energies[min(image.width-1,x+1)][y-1]!!.first))
                energies[x][y] = Pair(energies[x][y]!!.first + energies[x][y-1]!!.first, x)
            else if(energies[min(image.width-1,x+1)][y-1]!!.first <= min(energies[x][y-1]!!.first, energies[max(0,x-1)][y-1]!!.first))
                energies[x][y] = Pair(energies[x][y]!!.first + energies[min(image.width-1,x+1)][y-1]!!.first, min(image.width-1,x+1))
        }

    var curX = energies.indices.minByOrNull { energies[it][image.height-1]!!.first }?:throw IllegalArgumentException()
    for (y in image.height - 1 downTo 0) {
        for (x in 0 until outImage.width) {
            if(x < curX)
                outImage.setRGB(x, y, image.getRGB(x, y))
            else
                outImage.setRGB(x, y, image.getRGB(x+1, y))
        }
        if (y > 0) curX = energies[curX][y]!!.second
    }
    return outImage
}