package com.allogy.isqrl.components;

import org.apache.tapestry5.Binding;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BindingSource;

/**
 * Layout component for pages of application isqrl.
 */
//Import(stylesheet = "context:layout/layout.css")
public class Layout
{

    @Property
    //@Parameter(required = true, defaultPrefix = BindingConstants.MESSAGE)
    @Parameter
    private String title;

    @Inject
    private BindingSource bindingSource;

    @Inject
    private ComponentResources componentResources;

    Binding defaultTitle()
    {
        ComponentResources pageResources = componentResources.getPage().getComponentResources();
        return bindingSource.newBinding("title from subordinate component page class", pageResources, BindingConstants.MESSAGE, "title");
    }

}
