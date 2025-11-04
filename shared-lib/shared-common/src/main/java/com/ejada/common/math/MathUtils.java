package com.ejada.common.math;

import com.google.common.math.BigIntegerMath;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;

import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * A class for arithmetic on values of type {@code int , long , double , BigInteger}. Where possible, methods are defined and
 * named analogously to their {@code BigInteger} counterparts.
 *
 * <p>The implementations of all methods in this class are based on Google's Guava Library
 *
 * <p>Similar functionality for {@code long} and for {@link BigInteger} can be found in {@link
 * LongMath} and {@link BigIntegerMath} respectively.
 * Usage Example:
 * <br/>
 * - {@code MathUtils.IntUtils.factorial(10);}
 * <br/>
 * - {@code MathUtils.LongUtils.factorial(10);}
 * <br/>
 * - {@code MathUtils.DoubleUtils.factorial(10);}
 * <br/>
 * - {@code MathUtils.BigIntegerUtils.factorial(10);}
 */
public final class MathUtils {
    private MathUtils() {
    }

    public static final class IntUtils {
        private IntUtils() {
        }

        /**
         * This function calculates the binomial coefficient of n and k.
         * It makes sure that the result is within the integer range.
         * Otherwise, it gives the Integer.MAX_VALUE.
         * The answer can be derived using the formula n/k(n-k)
         * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
         * {@code k}, or {@link Integer#MAX_VALUE} if the result does not fit in an {@code int}.
         *
         * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}
         */
        public static int binomial(int n, int k) {
            return IntMath.binomial(n, k);
        }

        /**
         * Returns the smallest power of two greater than or equal to {@code x}. This is equivalent to
         * {@code checkedPow(2, log2(x, CEILING))}.
         * The result n is such that 2^(n-1) < x < 2 ^n
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      of the next-higher power of two is not representable as an {@code
         *                                  int}, i.e. when {@code x > 2^30}
         * @since 20.0
         */
        public static int ceilingPowerOfTwo(int x) {
            return IntMath.ceilingPowerOfTwo(x);
        }

        /**
         * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
         * {@code n == 0}, or {@link Integer#MAX_VALUE} if the result does not fit in a {@code int}.
         *
         * @throws IllegalArgumentException if {@code n < 0}
         */
        public static int factorial(int n) {
            return IntMath.factorial(n);
        }

        /**
         * Returns the largest power of two less than or equal to {@code x}. This is equivalent to {@code
         * checkedPow(2, log2(x, FLOOR))}.
         * Ex:-
         * {@code floorPowerOfTwo(30) = 16}
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @since 20.0
         */
        public static int floorPowerOfTwo(int x) {
            return IntMath.floorPowerOfTwo(x);
        }

        /**
         * Returns {@code true} if {@code x} represents a power of two.
         *
         * <p>This differs from {@code Integer.bitCount(x) == 1}, because {@code
         * Integer.bitCount(Integer.MIN_VALUE) == 1}, but {@link Integer#MIN_VALUE} is not a power of two.
         */
        public static boolean isPowerOfTwo(int x) {
            return IntMath.isPowerOfTwo(x);
        }


        /**
         * Returns the greatest common divisor of {@code a, b}. Returns {@code 0} if {@code a == 0 && b ==
         * 0}.
         *
         * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
         */
        public static int gcd(int a, int b) {
            return IntMath.gcd(a, b);
        }

        /**
         * Returns the least common multiple of {@code a, b}. Returns {@code 0} if {@code a == 0 && b ==
         * 0}.
         *
         * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
         */
        public static int lcm(int a, int b) {
            return IntMath.checkedMultiply(Math.abs(IntMath.divide(a, gcd(a, b), RoundingMode.HALF_EVEN)), Math.abs(b));
        }

        /**
         * Returns {@code true} if {@code n} is a <a
         * href="http://mathworld.wolfram.com/PrimeNumber.html">prime number</a>: an integer <i>greater
         * than one</i> that cannot be factored into a product of <i>smaller</i> positive integers.
         * Returns {@code false} if {@code n} is zero, one, or a composite number (one which <i>can</i> be
         * factored into smaller positive integers).
         *
         * @throws IllegalArgumentException if {@code n} is negative
         */
        public static boolean isPrime(int n) {
            return IntMath.isPrime(n);
        }

        /**
         * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of ten
         */
        public static int log10(int x, RoundingMode mode) {
            return IntMath.log10(x, mode);
        }

        /**
         * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of two
         */
        public static int log2(int x, RoundingMode mode) {
            return IntMath.log2(x, mode);
        }

        /**
         * Returns the arithmetic mean of {@code x} and {@code y}, rounded towards negative infinity. This
         * method is overflow resilient.
         */
        public static int mean(int x, int y) {
            return IntMath.mean(x, y);
        }

        /**
         * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from {@code x %
         * m}, which might be negative.
         *
         * <p>For example:
         *
         * <pre>{@code
         * mod(7, 4) == 3
         * mod(-7, 4) == 1
         * mod(-1, 4) == 3
         * mod(-8, 4) == 0
         * mod(8, 4) == 0
         * }</pre>
         *
         * @throws ArithmeticException if {@code m <= 0}
         * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
         * Remainder Operator</a>
         */
        public static int mod(int x, int m) {
            return IntMath.mod(x, m);
        }

        /**
         * Returns {@code b} to the {@code k}th power. Even if the result overflows, it will be equal to
         * {@code BigInteger.valueOf(b).pow(k).intValue()}. This implementation runs in {@code O(log k)}
         * time.
         *
         * @throws ArithmeticException      upon overflow.
         * @throws IllegalArgumentException if {@code k < 0}
         */
        public static int pow(int b, int k) {
            return IntMath.pow(b, k);
        }

        /**
         * Returns the square root of {@code x}, rounded with the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x < 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code
         *                                  sqrt(x)} is not an integer
         */
        public static int sqrt(int x, RoundingMode mode) {
            return IntMath.sqrt(x, mode);
        }

    }

    public static final class LongUtils {
        private LongUtils() {
        }

        /**
         * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
         * {@code k}, or {@link Long#MAX_VALUE} if the result does not fit in a {@code long}.
         *
         * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
         */
        public static long binomial(int n, int k) {
            return LongMath.binomial(n, k);
        }

        /**
         * Returns the smallest power of two greater than or equal to {@code x}. This is equivalent to
         * {@code checkedPow(2, log2(x, CEILING))}.
         * The result n is such that 2^(n-1) < x < 2 ^n
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      of the next-higher power of two is not representable as an {@code
         *                                  int}, i.e. when {@code x > 2^30}
         * @since 20.0
         */
        public static long ceilingPowerOfTwo(long x) {
            return LongMath.ceilingPowerOfTwo(x);
        }

        /**
         * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
         * {@code n == 0}, or {@link Integer#MAX_VALUE} if the result does not fit in a {@code int}.
         *
         * @throws IllegalArgumentException if {@code n < 0}
         */
        public static long factorial(int n) {
            return LongMath.factorial(n);
        }

        /**
         * Returns the largest power of two less than or equal to {@code x}. This is equivalent to {@code
         * checkedPow(2, log2(x, FLOOR))}.
         * Ex:-
         * {@code floorPowerOfTwo(30) = 16}
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @since 20.0
         */
        public static long floorPowerOfTwo(long x) {
            return LongMath.floorPowerOfTwo(x);
        }

        /**
         * Returns {@code true} if {@code x} represents a power of two.
         *
         * <p>This differs from {@code Integer.bitCount(x) == 1}, because {@code
         * Integer.bitCount(Integer.MIN_VALUE) == 1}, but {@link Integer#MIN_VALUE} is not a power of two.
         */
        public static boolean isPowerOfTwo(long x) {
            return LongMath.isPowerOfTwo(x);
        }


        /**
         * Returns the greatest common divisor of {@code a, b}. Returns {@code 0} if {@code a == 0 && b ==
         * 0}.
         *
         * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
         */
        public static long gcd(long a, long b) {
            return LongMath.gcd(a, b);
        }

        /**
         * Returns the least common multiple of {@code a, b}. Returns {@code 0} if {@code a == 0 && b ==
         * 0}.
         *
         * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
         */
        public static long lcm(long a, long b) {
            return LongMath.checkedMultiply(Math.abs(LongMath.divide(a, gcd(a, b), RoundingMode.HALF_EVEN)), Math.abs(b));
        }

        /**
         * Returns {@code true} if {@code n} is a <a
         * href="http://mathworld.wolfram.com/PrimeNumber.html">prime number</a>: an integer <i>greater
         * than one</i> that cannot be factored into a product of <i>smaller</i> positive integers.
         * Returns {@code false} if {@code n} is zero, one, or a composite number (one which <i>can</i> be
         * factored into smaller positive integers).
         *
         * @throws IllegalArgumentException if {@code n} is negative
         */
        public static boolean isPrime(long n) {
            return LongMath.isPrime(n);
        }

        /**
         * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of ten
         */
        public static long log10(long x, RoundingMode mode) {
            return LongMath.log10(x, mode);
        }

        /**
         * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of two
         */
        public static long log2(long x, RoundingMode mode) {
            return LongMath.log2(x, mode);
        }

        /**
         * Returns the arithmetic mean of {@code x} and {@code y}, rounded towards negative infinity. This
         * method is overflow resilient.
         */
        public static long mean(long x, long y) {
            return LongMath.mean(x, y);
        }

        /**
         * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from {@code x %
         * m}, which might be negative.
         *
         * <p>For example:
         *
         * <pre>{@code
         * mod(7, 4) == 3
         * mod(-7, 4) == 1
         * mod(-1, 4) == 3
         * mod(-8, 4) == 0
         * mod(8, 4) == 0
         * }</pre>
         *
         * @throws ArithmeticException if {@code m <= 0}
         * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
         * Remainder Operator</a>
         */
        public static long mod(long x, int m) {
            return LongMath.mod(x, m);
        }

        /**
         * Returns {@code b} to the {@code k}th power. Even if the result overflows, it will be equal to
         * {@code BigInteger.valueOf(b).pow(k).intValue()}. This implementation runs in {@code O(log k)}
         * time.
         *
         * @throws ArithmeticException      upon overflow.
         * @throws IllegalArgumentException if {@code k < 0}
         */
        public static long pow(long b, int k) {
            return LongMath.pow(b, k);
        }

        /**
         * Returns the square root of {@code x}, rounded with the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x < 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code
         *                                  sqrt(x)} is not an integer
         */
        public static long sqrt(long x, RoundingMode mode) {
            return LongMath.sqrt(x, mode);
        }


    }

    public static final class BigIntegerUtils {
        private BigIntegerUtils() {
        }

        /**
         * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
         * {@code k}, that is, {@code n! / (k! (n - k)!)}.
         *
         * <p><b>Warning:</b> the result can take as much as <i>O(k log n)</i> space.
         *
         * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
         */
        public static BigInteger binomial(int n, int k) {
            return BigIntegerMath.binomial(n, k);
        }

        /**
         * Returns the smallest power of two greater than or equal to {@code x}. This is equivalent to
         * {@code BigInteger.valueOf(2).pow(log2(x, CEILING))}.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         */
        public static BigInteger ceilingPowerOfTwo(BigInteger x) {
            return BigIntegerMath.ceilingPowerOfTwo(x);
        }

        /**
         * Returns {@code n!}, that is, the product of the first {@code n} positive integers, or {@code 1}
         * if {@code n == 0}.
         *
         * <p><b>Warning:</b> the result takes <i>O(n log n)</i> space, so use cautiously.
         *
         * <p>This uses an efficient binary recursive algorithm to compute the factorial with balanced
         * multiplies. It also removes all the 2s from the intermediate products (shifting them back in at
         * the end).
         *
         * @throws IllegalArgumentException if {@code n < 0}
         */
        public static BigInteger factorial(int n) {
            return BigIntegerMath.factorial(n);
        }

        /**
         * Returns the largest power of two less than or equal to {@code x}. This is equivalent to {@code
         * BigInteger.valueOf(2).pow(log2(x, FLOOR))}.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         */
        public static BigInteger floorPowerOfTwo(BigInteger x) {
            return BigIntegerMath.floorPowerOfTwo(x);
        }

        /**
         * Returns {@code true} if {@code x} represents a power of two.
         */
        public static boolean isPowerOfTwo(BigInteger x) {
            return BigIntegerMath.isPowerOfTwo(x);
        }

        /**
         * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of ten
         */
        public static int log10(BigInteger x, RoundingMode mode) {
            return BigIntegerMath.log10(x, mode);
        }

        /**
         * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x <= 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
         *                                  is not a power of two
         */
        public static int log2(BigInteger x, RoundingMode mode) {
            return BigIntegerMath.log2(x, mode);
        }

        /**
         * Returns the square root of {@code x}, rounded with the specified rounding mode.
         *
         * @throws IllegalArgumentException if {@code x < 0}
         * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code
         *                                  sqrt(x)} is not an integer
         */
        public static BigInteger sqrt(BigInteger x, RoundingMode mode) {
            return BigIntegerMath.sqrt(x, mode);
        }
    }

    public static final class DoubleUtils {
        private DoubleUtils() {
        }

        /**
         * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
         * {@code n == 0}, or {@code n!}, or {@link Double#POSITIVE_INFINITY} if {@code n! >
         * Double.MAX_VALUE}.
         *
         * <p>The result is within 1 ulp of the true value.
         *
         * @throws IllegalArgumentException if {@code n < 0}
         */
        public static double factorial(int n) {
            return DoubleMath.factorial(n);
        }

        /**
         * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
         * {@code int}.
         *
         * <p>Regardless of the rounding mode, this is faster than {@code (int) log2(x)}.
         *
         * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is
         *                                  infinite
         */
        public static double log2(double x, RoundingMode mode) {
            return DoubleMath.log2(x, mode);
        }

        /**
         * Returns the base 2 logarithm of a double value.
         *
         * <p>Special cases:
         *
         * <ul>
         *   <li>If {@code x} is NaN or less than zero, the result is NaN.
         *   <li>If {@code x} is positive infinity, the result is positive infinity.
         *   <li>If {@code x} is positive or negative zero, the result is negative infinity.
         * </ul>
         *
         * <p>The computed result is within 1 ulp of the exact result.
         *
         * <p>If the result of this method will be immediately rounded to an {@code int}, {@link
         * #log2(double, RoundingMode)} is faster.
         */
        public static double log2(double x) {
            return DoubleMath.log2(x);
        }

        /**
         * Returns the {@code int} value that is equal to {@code x} rounded with the specified rounding
         * mode, if possible.
         *
         * @throws ArithmeticException if
         *                             <ul>
         *                               <li>{@code x} is infinite or NaN
         *                               <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
         *                                   mode, is either less than {@code Integer.MIN_VALUE} or greater than {@code
         *                                   Integer.MAX_VALUE}
         *                               <li>{@code x} is not a mathematical integer and {@code mode} is {@link
         *                                   RoundingMode#UNNECESSARY}
         *                             </ul>
         */
        public static int roundToInt(double x, RoundingMode mode) {
            return DoubleMath.roundToInt(x, mode);
        }

        /**
         * Returns the {@code long} value that is equal to {@code x} rounded with the specified rounding
         * mode, if possible.
         *
         * @throws ArithmeticException if
         *                             <ul>
         *                               <li>{@code x} is infinite or NaN
         *                               <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
         *                                   mode, is either less than {@code Long.MIN_VALUE} or greater than {@code
         *                                   Long.MAX_VALUE}
         *                               <li>{@code x} is not a mathematical integer and {@code mode} is {@link
         *                                   RoundingMode#UNNECESSARY}
         *                             </ul>
         */
        public static long roundToLong(double x, RoundingMode mode) {
            return DoubleMath.roundToLong(x, mode);
        }

    }
}
