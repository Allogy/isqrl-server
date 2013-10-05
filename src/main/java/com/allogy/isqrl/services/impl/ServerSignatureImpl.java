package com.allogy.isqrl.services.impl;

import com.allogy.isqrl.services.RandomSource;
import com.allogy.isqrl.services.ServerSignature;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 3:24 PM
 */
@EagerLoad
public class ServerSignatureImpl implements ServerSignature
{

    private String superSecretServerKeyThatShouldNeverBeLeakedOrChanged;

    @Inject
    private Logger log;

    @Inject
    private RandomSource randomSource;

    private final
    ThreadLocal<MessageDigest> sha1Source=new ThreadLocal<MessageDigest>()
    {
        @Override
        protected
        MessageDigest initialValue()
        {
            try {
                return MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @PostInjection
    public
    void serviceStarts() throws InterruptedException
    {
        String secret=System.getProperty("ISQRL_SERVER_SECRET");

        if (secret==null)
        {
            secret=System.getenv("ISQRL_SERVER_SECRET");
        }

        if (secret==null)
        {
            log.warn("ISQRL_SERVER_SECRET is not defined, the service will now generate one for you, but if the service ever restarts all clients will get an ugly warning, so I hope this is not a production instance...");

            Thread.sleep(2000);
            secret=randomLongishString();

            log.error("was not configured, so we have generated a ISQRL_SERVER_SECRET for you... but you will probably want to save this somewhere: {}", secret);
        }

        if (secret.indexOf(':')>=0)
        {
            throw new IllegalArgumentException("ISQRL_SERVER_SECRET may not contain a colon");
        }

        superSecretServerKeyThatShouldNeverBeLeakedOrChanged=secret;
    }

    public
    String sha1HexOf(String input)
    {
        try {
            byte[] bytes=sha1Source.get().digest(input.getBytes("UTF-8"));
            return byteArrayToHexString(bytes, 0, bytes.length);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static
    String byteArrayToHexString(byte[] b, int offset, int length)
    {
        StringBuilder result = new StringBuilder();
        for (int i=0; i < length; i++) {
            result.append(Integer.toString((b[i + offset] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public
    String randomLongishString()
    {
        Random random=randomSource.get();
        return randomString(random, 20+random.nextInt(20));
    }

    private
    String randomString(Random random, int length)
    {
        StringBuilder sb=new StringBuilder();

        int range=10+26+26;
        for (int n=length; n>0; n--)
        {
            int i=random.nextInt(range);
            char c;
            if (i>=10+26)
            {
                c=(char)('A'+(i-(10+26)));
            }
            else
            if (i>=10)
            {
                c=(char)('a'+(i-10));
            }
            else
            {
                c=(char)('0'+(i));
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public
    String prependSignature(String z)
    {
        String signature=sha1HexOf(superSecretServerKeyThatShouldNeverBeLeakedOrChanged+z);
        return signature+":"+z;
    }

    public
    String randomButMemorableString()
    {
        Random random=randomSource.get();
        int first=random.nextInt(5)+1;
        return randomString(random, first)+"-"+randomString(random, 6-first);
    }

    public
    boolean prependedSignatureMatches(String signedZ)
    {
        String z=removePrependedSignature(signedZ);
        log.trace("strip: {} -> {}", signedZ, z);

        String expectedSignature=sha1HexOf(superSecretServerKeyThatShouldNeverBeLeakedOrChanged+z);
        log.trace("match? expecting '{}' as a prefix to '{}'", expectedSignature, signedZ);

        return signedZ.startsWith(expectedSignature);
    }

    public
    String removePrependedSignature(String signedZ)
    {
        int colon=signedZ.indexOf(':');

        if (colon<=0)
        {
            return null;
        }
        else
        {
            return signedZ.substring(colon+1);
        }
    }

    public
    boolean hashOfYMatchesHashY(String y, String hashY)
    {
        String computedHashY=sha1HexOf(y);
        return computedHashY.equals(hashY);
    }

}
