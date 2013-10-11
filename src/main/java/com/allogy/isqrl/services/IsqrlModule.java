package com.allogy.isqrl.services;

import com.allogy.isqrl.helpers.OutputStreamResponse;
import com.allogy.isqrl.services.impl.CrossRoadsImpl;
import com.allogy.isqrl.services.impl.DefaultJavaScriptAndCssCensor;
import com.allogy.isqrl.services.impl.RandomSourceImpl;
import com.allogy.isqrl.services.impl.ServerSignatureImpl;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Decorate;
import org.apache.tapestry5.services.ComponentEventResultProcessor;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.javascript.JavaScriptStackSource;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public class IsqrlModule
{
    public static final String VERSION="0.3";

    public static void bind(ServiceBinder binder)
    {
        binder.bind(CrossRoads.class     , CrossRoadsImpl.class     );
        binder.bind(RandomSource.class   , RandomSourceImpl.class   );
        binder.bind(ServerSignature.class, ServerSignatureImpl.class);
    }

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        // The application version number is incorprated into URLs for some
        // assets. Web browsers will cache assets because of the far future expires
        // header. If existing assets are changed, the version number should also
        // change, to force the browser to download new versions. This overrides Tapesty's default
        // (a random hexadecimal number), but may be further overriden by DevelopmentModule or
        // QaModule.
        configuration.override(SymbolConstants.APPLICATION_VERSION, VERSION);
    }

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        // Contributions to ApplicationDefaults will override any contributions to
        // FactoryDefaults (with the same key). Here we're restricting the supported
        // locales to just "en" (English). As you add localised message catalogs and other assets,
        // you can extend this list of locales (it's a comma separated series of locale names;
        // the first locale name is the default when there's no reasonable match).
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en");

        configuration.add(SymbolConstants.SECURE_ENABLED, "false");

        configuration.add(SymbolConstants.DEFAULT_STYLESHEET, "context:main.css");
    }

    /**
     * When in production mode, all pages & actions should be presumed as "secure" (using HTTPS).
     * Otherwise, Tapestry will try and over think what pages should be secure.
     *
     * @url http://tapestry.apache.org/https.html
     * @param configuration
     */
    public static
    void contributeMetaDataLocator(MappedConfiguration<String,String> configuration)
    {
        configuration.add(MetaDataConstants.SECURE_PAGE, "true");
    }

    /**
     * This is a service definition, the service will be named "TimingFilter". The interface,
     * RequestFilter, is used within the RequestHandler service pipeline, which is built from the
     * RequestHandler service configuration. Tapestry IoC is responsible for passing in an
     * appropriate Logger instance. Requests for static resources are handled at a higher level, so
     * this filter will only be invoked for Tapestry related requests.
     * <p/>
     * <p/>
     * Service builder methods are useful when the implementation is inline as an inner class
     * (as here) or require some other kind of special initialization. In most cases,
     * use the static bind() method instead.
     * <p/>
     * <p/>
     * If this method was named "build", then the service id would be taken from the
     * service interface and would be "RequestFilter".  Since Tapestry already defines
     * a service named "RequestFilter" we use an explicit service id that we can reference
     * inside the contribution method.
     * /
    public RequestFilter buildTimingFilter(final Logger log)
    {
        return new RequestFilter()
        {
            public boolean service(Request request, Response response, RequestHandler handler)
                    throws IOException
            {
                long startTime = System.currentTimeMillis();

                try
                {
                    // The responsibility of a filter is to invoke the corresponding method
                    // in the handler. When you chain multiple filters together, each filter
                    // received a handler that is a bridge to the next filter.

                    return handler.service(request, response);
                } finally
                {
                    long elapsed = System.currentTimeMillis() - startTime;

                    log.info(String.format("Request time: %d ms", elapsed));
                }
            }
        };
    }

    /**
     * This is a contribution to the RequestHandler service configuration. This is how we extend
     * Tapestry using the timing filter. A common use for this kind of filter is transaction
     * management or security. The @Local annotation selects the desired service by type, but only
     * from the same module.  Without @Local, there would be an error due to the other service(s)
     * that implement RequestFilter (defined in other modules).
     * /
    public void contributeRequestHandler(OrderedConfiguration<RequestFilter> configuration,
                                         @Local
                                         RequestFilter filter)
    {
        // Each contribution to an ordered configuration has a name, When necessary, you may
        // set constraints to precisely control the invocation order of the contributed filter
        // within the pipeline.

        configuration.add("Timing", filter);
    }
    */

    /**
     * Adds ComponentEventResultProcessors
     *
     * @param configuration the configuration where new ComponentEventResultProcessors are registered by the type they are processing
     * @param response the response that the event result processor handles
     * @url http://wiki.apache.org/tapestry/Tapestry5HowToCreateAComponentEventResultProcessor
     */
    public static
    void contributeComponentEventResultProcessor(MappedConfiguration<Class<?>, ComponentEventResultProcessor<?>> configuration, Response response)
    {
        configuration.add(OutputStreamResponse.class, new OutputStreamResponseResultProcessor(response));
    }

    @Decorate(serviceInterface = JavaScriptStackSource.class)
    public static
    JavaScriptStackSource decorateJavaScriptStackSource(JavaScriptStackSource original)
    {
        return new DefaultJavaScriptAndCssCensor(original);
    }
}
