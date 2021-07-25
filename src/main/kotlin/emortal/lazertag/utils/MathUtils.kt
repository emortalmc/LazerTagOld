package emortal.lazertag.utils

object MathUtils {
    fun digitsInNumber(n: Int): Int {
        // https://stackoverflow.com/questions/1306727/way-to-get-number-of-digits-in-an-int
        // No fucking idea why this is the fastest method

        return if (n < 100000) {
            if (n < 100){
                if (n < 10) 1 else 2
            } else {
                if (n < 1000) 3
                else {
                    if (n < 10000) 4 else 5
                }
            }
        } else {
            if (n < 10000000) {
                if (n < 1000000) 6 else 7
            } else {
                if (n < 100000000) 8
                else {
                    if (n < 1000000000) 9 else 10
                }
            }
        }
    }
}