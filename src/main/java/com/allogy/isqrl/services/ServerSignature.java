package com.allogy.isqrl.services;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 3:22 PM
 */
public interface ServerSignature
{
    String prependSignature(String z);

    String randomButMemorableString();

    String randomLongishString();

    boolean prependedSignatureMatches(String signedZ);

    String removePrependedSignature(String signedZ);

    boolean hashOfYMatchesHashY(String y, String hashY);

    String sha1HexOf(String input);
}
