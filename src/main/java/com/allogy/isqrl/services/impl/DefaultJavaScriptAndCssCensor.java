package com.allogy.isqrl.services.impl;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.services.javascript.JavaScriptStack;
import org.apache.tapestry5.services.javascript.JavaScriptStackSource;
import org.apache.tapestry5.services.javascript.StylesheetLink;

import java.util.*;

/**
 * @url http://stackoverflow.com/questions/12991875/remove-generic-tapestry-javascript-and-css
 *
 * User: robert
 * Date: 2013/10/09
 * Time: 11:34 AM
 */
public class DefaultJavaScriptAndCssCensor implements JavaScriptStackSource
{
    // This bunch of stacks comes from got5
    private Set<String> SKIP = new HashSet<String>(Arrays.asList(
            "Slider",
            "AjaxUploadStack",
            "CoreJavaScriptStack",
            "DataTableStack",
            "FormFragmentSupportStack",
            "FormSupportStack",
            "SuperfishStack",
            "JQueryDateFieldStack",
            "GalleryStack"
    ));

    private static
    class JavaScriptStackWraper implements JavaScriptStack
    {
        private final JavaScriptStack original;

        JavaScriptStackWraper(JavaScriptStack original) {
            if (original != null) {
                System.out.println("Wrap " + original.getClass().getName());
            }
            this.original = original;
        }

        public
        List<String> getStacks() {
            return original != null ? original.getStacks() : Collections.<String>emptyList();
        }

        public
        List<Asset> getJavaScriptLibraries() {
            return original != null ? original.getJavaScriptLibraries() : Collections.<Asset>emptyList();
        }

        // Always return empty list
        public
        List<StylesheetLink> getStylesheets() {
            return Collections.<StylesheetLink>emptyList();
        }

        public
        String getInitialization() {
            return original != null ? original.getInitialization() : null;
        }
    }

    private final JavaScriptStackSource original;

    public
    DefaultJavaScriptAndCssCensor(JavaScriptStackSource original)
    {
        this.original = original;
    }

    public
    JavaScriptStack getStack(String name) {
        JavaScriptStack stack = original.getStack(name);
        if (!SKIP.contains(stack.getClass().getSimpleName())) {
            return new JavaScriptStackWraper(stack);
        }
        return new JavaScriptStackWraper(null);
    }

    public
    List<String> getStackNames() {
        return original.getStackNames();
    }

}
