package com.github.morj.wobot

import java.awt.Point
import java.io.InputStreamReader

fun sumAtLeast(tolerance: Int, vararg f: () -> Int): Int {
    var result = 0
    for (i in 0..f.size - 1) {
        result += f[i]()
        if (result > tolerance) {
            break
        }
    }
    return result
}

operator fun Point.div(other: Int): Point = Point(x / other, y / other)

fun data(path: String) = InputStreamReader(Wobot::class.java.getResourceAsStream("$path.txt")).readLines()
