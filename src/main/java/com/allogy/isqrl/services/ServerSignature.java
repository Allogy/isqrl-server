package com.allogy.isqrl.services;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 3:22 PM
 */
public interface ServerSignature
{
    String prependSignature(String z);

    String randomLongishString();

    boolean prependedSignatureMatches(String signedZ);

    String removePrependedSignature(String signedZ);
}
