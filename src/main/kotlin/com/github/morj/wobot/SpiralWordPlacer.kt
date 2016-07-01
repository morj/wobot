package com.github.morj.wobot

import com.kennycason.kumo.Word
import com.kennycason.kumo.placement.RectangleWordPlacer
import java.awt.Dimension
import java.awt.Point

// author: Pavel Nikitin, 2016
// based on works of Mike Talbot
// see http://stackoverflow.com/questions/342687/algorithm-to-implement-a-word-cloud-like-wordle
// and https://github.com/whydoidoit/WordCloud

class SpiralWordPlacer(/*val source: BufferedImage,*/ dimension: Dimension) : RectangleWordPlacer {
    val width: Int = dimension.width
    val height: Int = dimension.height
    val points = listOf(
            width / 2 to height / 2,
            width / 4 to height / 4,
            width / 4 to 3 * height / 2,
            3 * width / 4 to height / 2,
            3 * width / 4 to 3 * height / 4
    ).map { Point(it.first, it.second) }

    var mask = Array(((width / 4) + 2) * ((height / 4) + 2)) { -1 }
    var currentPoint = 0
    var center = points[currentPoint]
    var position = 0.toDouble()
    var index = 0

    override fun place(word: Word): Boolean {
        val lst = createCollisionList(word)
        while (true) {
            val spiralPoint = getSpiralPoint(position)
            val offsetX = word.dimension.width / 2
            val offsetY = word.dimension.height / 2
            val testPoint = Point(spiralPoint.x + center.x - offsetX, (spiralPoint.y + center.y - offsetY))
            if (position > 2 * Math.PI * 580) {
                if (++currentPoint >= points.size) {
                    return false
                }
                position = 0.0
                center = points[currentPoint]
                continue
            }
            val cols = countCollisions(testPoint, lst, 2)
            if (cols == 0) {
                placePoint(testPoint, lst, offsetX, offsetY)
                copyBits(testPoint, lst, index++)
                word.position.x = testPoint.x
                word.position.y = testPoint.y
                break
            } else {
                position += if (cols <= 2) {
                    2 * Math.PI / 100
                } else {
                    2 * Math.PI / 40
                }
            }
        }

        currentPoint++

        return true
    }

    private fun placePoint(testPoint: Point, lst: Map<Point, List<Point>>, offsetX: Int, offsetY: Int) {
        while (true) {
            val oldY = testPoint.y
            if (Math.abs(testPoint.x + offsetX - center.x) > 10) {
                if (testPoint.x + offsetX < center.x) {
                    do {
                        testPoint.x += 2
                    } while (testPoint.x + offsetX < center.x && countCollisions(testPoint, lst, 0) == 0)
                    testPoint.x -= 2
                } else {
                    do {
                        testPoint.x -= 2
                    } while (testPoint.x + offsetX > center.x && countCollisions(testPoint, lst, 0) == 0)
                    testPoint.x += 2
                }
            }
            if (Math.abs(testPoint.y + offsetY - center.y) > 10) {
                if (testPoint.y + offsetY < center.y) {
                    do {
                        testPoint.y += 2
                    } while (testPoint.y + offsetY < center.y && countCollisions(testPoint, lst, 0) == 0)
                    testPoint.y -= 2
                } else {
                    do {
                        testPoint.y -= 2
                    } while (testPoint.y + offsetY > center.y && countCollisions(testPoint, lst, 0) == 0)
                    testPoint.y += 2
                }
                if (testPoint.y != oldY) {
                    continue
                }
            }
            break
        }
    }

    private fun createCollisionList(word: Word): Map<Point, List<Point>> {
        val pixelHeight = word.dimension.height
        val pixelWidth = word.dimension.width
        val lookup = mutableMapOf<Point, List<Point>>()

        for (y in 0..pixelHeight - 1) {
            for (x in 0..pixelWidth - 1) {
                if (!word.collisionRaster.isTransparent(x, y)) {
                    val detailPoint = Point(x, y)
                    val blockPoint = detailPoint / 4
                    lookup[blockPoint] ?: mutableListOf<Point>().apply {
                        lookup[blockPoint] = this
                    }.add(detailPoint)
                    // print('\u2588')
                } else {
                    // print(' ')
                }
            }
            // println()
        }
        return lookup
    }

    private fun copyBits(testPoint: Point, lst: Map<Point, List<Point>>, index: Int) {
        val maskWidth = width / 4
        val s = testPoint / 4
        lst.keys.forEach {
            mask[(it.y + s.y) * maskWidth + (it.x + s.x)] = index
            mask[(it.y + s.y + 1) * maskWidth + (it.x + s.x)] = index
            mask[(it.y + s.y + 1) * maskWidth + (it.x + 1 + s.x)] = index
            mask[(it.y + s.y) * maskWidth + (it.x + 1 + s.x)] = index
        }
    }

    private fun getSpiralPoint(position: Double, r: Double = 7.toDouble()): Point {
        val radius = position / (2 * Math.PI) * r
        val angle = position % (2 * Math.PI)
        return Point((radius * Math.sin(angle)).toInt(), (radius * Math.cos(angle)).toInt())
    }

    private fun countCollisions(testPoint: Point, lst: Map<Point, List<Point>>, tolerance: Int): Int {
        return sumAtLeast(tolerance, {
            getCollisions(Point(testPoint.x + 2, testPoint.y), lst)
        }, {
            getCollisions(Point(testPoint.x - 2, testPoint.y), lst)
        }, {
            getCollisions(testPoint, lst)
        }, {
            getCollisions(Point(testPoint.x, testPoint.y + 2), lst)
        }, {
            getCollisions(Point(testPoint.x, testPoint.y - 2), lst)
        })
    }

    private fun getCollisions(pt: Point, intersections: Map<Point, List<Point>>): Int {
        // val pixels = (source.raster.dataBuffer as DataBufferInt).data
        val maskWidth = width / 4

        return if (intersections.any {
            collide(/*pixels,*/ it.key, pt, it.value, maskWidth)
        }) {
            1
        } else {
            0
        }
    }

    private fun collide(/*pixels: IntArray,*/ a: Point, b: Point, list: List<Point>, maskWidth: Int): Boolean {
        val testPt = Point(b.x + a.x * 4, b.y + a.y * 4)
        if (testPt.x < 0 || testPt.x >= width || testPt.y < 0 || testPt.y >= height)
            return true
        val pos = (a.y + (b.y / 4)) * maskWidth + a.x + (b.x / 4)
        try {
            if (mask[pos] != -1 || mask[pos + 1] != -1 || mask[pos + maskWidth] != -1 || mask[pos + maskWidth + 1] != -1) {
                list.forEach {
                    if (it.x + b.x < 0 || it.x + b.x >= width || it.y + b.y < 0 || it.y + b.y >= height)
                        return true
                    throw IllegalStateException()
                    // if (pixels[(b.y + it.y) * width + (b.x + it.x)] != 0) return true
                }
            }
        } catch (e: Exception) {
            return true
        }
        return false
    }

    override fun reset() {
        //TODO
    }
}
