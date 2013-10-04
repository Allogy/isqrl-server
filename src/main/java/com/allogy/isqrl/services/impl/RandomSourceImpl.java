package com.allogy.isqrl.services.impl;

import com.allogy.isqrl.services.RandomSource;

import java.util.Random;

/**
 * Mimicks ThreadLocalRandom class... sorta.
 * If the threads live for multiple requests (like in any good servlet container), then this
 * lowers the overhead of getting a PRNG, lets us avoid synchronization, and helps keep the
 * PRNG state from being guessable (by default it is initialized with millisecond precision).
 *
 * User: robert
 * Date: 2013/10/04
 * Time: 3:31 PM
 */
public class RandomSourceImpl extends ThreadLocal<Random> implements RandomSource
{
    @Override
    protected
    Random initialValue()
    {
        return new Random(System.nanoTime());
    }

}
